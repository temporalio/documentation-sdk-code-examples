//! Runs the "Workflow Timeouts" examples against a local dev server.
//!
//! Set TEMPORAL_CLI_PATH to use an installed Temporal CLI instead of downloading one.

use temporalio_client::{Client, UntypedWorkflow, WorkflowFetchHistoryOptions};
use temporalio_common::protos::temporal::api::history::v1::{
    WorkflowExecutionStartedEventAttributes, history_event::Attributes,
};
use temporalio_sdk::{
    Runtime, Worker, WorkerOptions,
    testing::{EphemeralExe, LocalServer, LocalWorkflowEnvironmentOptions, WorkflowEnvironment},
};
use timeouts::{
    client::{start_with_retry_policy, start_with_timeouts},
    workflows::GreetingsWorkflow,
};

const TASK_QUEUE: &str = "my-task-queue";

async fn start_env() -> WorkflowEnvironment<LocalServer> {
    let mut options = LocalWorkflowEnvironmentOptions::default();
    if let Ok(path) = std::env::var("TEMPORAL_CLI_PATH") {
        options.server_executable = EphemeralExe::ExistingPath(path);
    }
    WorkflowEnvironment::start_local(options).await.unwrap()
}

/// Runs a Worker while `f` runs, then shuts the Worker down.
async fn with_worker<T>(client: &Client, f: impl AsyncFnOnce() -> T) -> T {
    let runtime = Runtime::from_current_tokio(Default::default()).unwrap();
    let worker_options = WorkerOptions::new(TASK_QUEUE)
        .register_workflow::<GreetingsWorkflow>()
        .unwrap()
        .build();
    let mut worker = Worker::new(&runtime, client.clone(), worker_options).unwrap();
    let shutdown = worker.shutdown_handle();
    let (result, run) = tokio::join!(
        async {
            let result = f().await;
            shutdown();
            result
        },
        worker.run()
    );
    run.unwrap();
    result
}

/// Returns the attributes of the Workflow's WorkflowExecutionStarted event.
async fn started_attributes(
    client: &Client,
    workflow_id: &str,
) -> WorkflowExecutionStartedEventAttributes {
    let events = client
        .get_workflow_handle::<UntypedWorkflow>(workflow_id)
        .fetch_history(WorkflowFetchHistoryOptions::default())
        .into_events()
        .await
        .unwrap();
    match events.into_iter().next().unwrap().attributes {
        Some(Attributes::WorkflowExecutionStartedEventAttributes(attributes)) => attributes,
        other => panic!("unexpected first event: {other:?}"),
    }
}

#[tokio::test]
async fn start_with_timeouts_sets_timeouts() {
    let env = start_env().await;
    let client = env.client().clone();
    with_worker(&client, async || {
        let result = start_with_timeouts(&client).await.unwrap();
        assert_eq!(result, "Hello, Temporal!");

        let attributes = started_attributes(&client, "greetings-workflow-10").await;
        assert_eq!(attributes.workflow_execution_timeout.unwrap().seconds, 3600);
        assert_eq!(attributes.workflow_run_timeout.unwrap().seconds, 600);
        assert_eq!(attributes.workflow_task_timeout.unwrap().seconds, 10);
    })
    .await;
    env.shutdown().await.unwrap();
}

#[tokio::test]
async fn start_with_retry_policy_sets_retry_policy() {
    let env = start_env().await;
    let client = env.client().clone();
    with_worker(&client, async || {
        let result = start_with_retry_policy(&client).await.unwrap();
        assert_eq!(result, "Hello, Temporal!");

        let attributes = started_attributes(&client, "greetings-workflow-11").await;
        let policy = attributes.retry_policy.unwrap();
        assert_eq!(policy.initial_interval.unwrap().seconds, 1);
        assert_eq!(policy.backoff_coefficient, 2.0);
        assert_eq!(policy.maximum_interval.unwrap().seconds, 100);
        assert_eq!(policy.maximum_attempts, 5);
        assert_eq!(policy.non_retryable_error_types, vec!["NonRetryableError"]);
    })
    .await;
    env.shutdown().await.unwrap();
}

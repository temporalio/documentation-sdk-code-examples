//! Runs the "Activity Execution" examples against a local dev server.
//!
//! Set TEMPORAL_CLI_PATH to use an installed Temporal CLI instead of downloading one.

use activity_execution::{
    activities::MyActivities, concurrent::ConcurrentGreetingWorkflow,
    schedule_to_close::GreetingWorkflow,
};
use temporalio_client::{
    Client, UntypedWorkflow, WorkflowFetchHistoryOptions, WorkflowGetResultOptions,
    WorkflowStartOptions,
};
use temporalio_common::protos::temporal::api::history::v1::{
    ActivityTaskScheduledEventAttributes, history_event::Attributes,
};
use temporalio_sdk::{
    Runtime, Worker, WorkerOptions,
    testing::{EphemeralExe, LocalServer, LocalWorkflowEnvironmentOptions, WorkflowEnvironment},
};

const TASK_QUEUE: &str = "activity-execution";

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
        .register_workflow::<GreetingWorkflow>()
        .unwrap()
        .register_workflow::<ConcurrentGreetingWorkflow>()
        .unwrap()
        .register_activities(MyActivities)
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

/// Returns the attributes of each ActivityTaskScheduled event.
async fn scheduled_activities(
    client: &Client,
    workflow_id: &str,
) -> Vec<ActivityTaskScheduledEventAttributes> {
    client
        .get_workflow_handle::<UntypedWorkflow>(workflow_id)
        .fetch_history(WorkflowFetchHistoryOptions::default())
        .into_events()
        .await
        .unwrap()
        .into_iter()
        .filter_map(|event| match event.attributes {
            Some(Attributes::ActivityTaskScheduledEventAttributes(attributes)) => Some(attributes),
            _ => None,
        })
        .collect()
}

#[tokio::test]
async fn schedule_to_close_timeout_is_set() {
    let env = start_env().await;
    let client = env.client().clone();
    with_worker(&client, async || {
        let handle = client
            .start_workflow(
                GreetingWorkflow::run,
                "Temporal".to_string(),
                WorkflowStartOptions::new(TASK_QUEUE, "schedule-to-close").build(),
            )
            .await
            .unwrap();
        let result = handle
            .get_result(WorkflowGetResultOptions::default())
            .await
            .unwrap();
        assert_eq!(result, "Hello, Temporal!");

        let scheduled = scheduled_activities(&client, "schedule-to-close").await;
        assert_eq!(scheduled.len(), 1);
        assert_eq!(scheduled[0].schedule_to_close_timeout.unwrap().seconds, 30);
    })
    .await;
    env.shutdown().await.unwrap();
}

#[tokio::test]
async fn activities_run_concurrently() {
    let env = start_env().await;
    let client = env.client().clone();
    with_worker(&client, async || {
        let handle = client
            .start_workflow(
                ConcurrentGreetingWorkflow::run,
                "Temporal".to_string(),
                WorkflowStartOptions::new(TASK_QUEUE, "concurrent").build(),
            )
            .await
            .unwrap();
        let result = handle
            .get_result(WorkflowGetResultOptions::default())
            .await
            .unwrap();
        assert_eq!(result, "Hello, Temporal! (English)");

        // Both Activities are scheduled by the same Workflow Task.
        let scheduled = scheduled_activities(&client, "concurrent").await;
        assert_eq!(scheduled.len(), 2);
        assert_eq!(
            scheduled[0].workflow_task_completed_event_id,
            scheduled[1].workflow_task_completed_event_id
        );
    })
    .await;
    env.shutdown().await.unwrap();
}

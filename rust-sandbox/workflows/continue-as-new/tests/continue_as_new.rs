//! Runs each "Continue-As-New" example against a local dev server.
//!
//! Set TEMPORAL_CLI_PATH to use an installed Temporal CLI instead of downloading one.

use continue_as_new::{
    activities::MyActivities,
    basic::GreetingWorkflow,
    test_hook::{GreetingInput, RepeatGreetingWorkflow},
};
use temporalio_client::{
    Client, UntypedWorkflow, WorkflowDescribeOptions, WorkflowExecutionStatus,
    WorkflowGetResultOptions, WorkflowStartOptions,
};
use temporalio_sdk::{
    Runtime, Worker, WorkerOptions,
    testing::{EphemeralExe, LocalWorkflowEnvironmentOptions, WorkflowEnvironment},
};

const TASK_QUEUE: &str = "continue-as-new";

/// Starts a dev server and a Worker, runs `f`, then shuts both down.
async fn with_worker<T>(f: impl AsyncFnOnce(Client) -> T) -> T {
    let mut options = LocalWorkflowEnvironmentOptions::default();
    if let Ok(path) = std::env::var("TEMPORAL_CLI_PATH") {
        options.server_executable = EphemeralExe::ExistingPath(path);
    }
    let env = WorkflowEnvironment::start_local(options).await.unwrap();
    let client = env.client().clone();

    let runtime = Runtime::from_current_tokio(Default::default()).unwrap();
    let worker_options = WorkerOptions::new(TASK_QUEUE)
        .register_activities(MyActivities)
        .register_workflow::<GreetingWorkflow>()
        .unwrap()
        .register_workflow::<RepeatGreetingWorkflow>()
        .unwrap()
        .build();
    let mut worker = Worker::new(&runtime, client.clone(), worker_options).unwrap();
    let shutdown = worker.shutdown_handle();

    let (result, run) = tokio::join!(
        async {
            let result = f(client).await;
            shutdown();
            result
        },
        worker.run()
    );
    run.unwrap();
    env.shutdown().await.unwrap();
    result
}

#[tokio::test]
async fn basic_continues_as_new_once_then_completes() {
    with_worker(async |client| {
        let handle = client
            .start_workflow(
                GreetingWorkflow::run,
                "Temporal".to_string(),
                WorkflowStartOptions::new(TASK_QUEUE, "basic").build(),
            )
            .await
            .unwrap();
        let first_run_id = handle.info().run_id.clone().unwrap();

        // get_result follows the chain to the last run.
        let result = handle.get_result(WorkflowGetResultOptions::default()).await.unwrap();
        assert_eq!(result, "Hello, Ziggy!");

        // The start handle targets the first run in the chain.
        let status = handle.describe(WorkflowDescribeOptions::default()).await.unwrap().status();
        assert_eq!(status, WorkflowExecutionStatus::ContinuedAsNew);

        let latest = client
            .get_workflow_handle::<UntypedWorkflow>("basic")
            .describe(WorkflowDescribeOptions::default())
            .await
            .unwrap();
        assert_ne!(latest.run_id(), first_run_id);
        assert_eq!(latest.workflow_type(), "greeting-workflow-1");
    })
    .await;
}

#[tokio::test]
async fn test_hook_continues_as_new_when_history_is_long() {
    with_worker(async |client| {
        let input = GreetingInput {
            name: "Ziggy".to_string(),
            greetings_remaining: 5,
            max_history_length: 10,
        };
        let handle = client
            .start_workflow(
                RepeatGreetingWorkflow::run,
                input,
                WorkflowStartOptions::new(TASK_QUEUE, "test-hook").build(),
            )
            .await
            .unwrap();
        let result = handle.get_result(WorkflowGetResultOptions::default()).await.unwrap();
        assert_eq!(result, "Sent all greetings to Ziggy");

        // The start handle targets the first run in the chain.
        let status = handle.describe(WorkflowDescribeOptions::default()).await.unwrap().status();
        assert_eq!(status, WorkflowExecutionStatus::ContinuedAsNew);
    })
    .await;
}

#[tokio::test]
async fn test_hook_disabled_keeps_one_run() {
    with_worker(async |client| {
        let input = GreetingInput {
            name: "Ziggy".to_string(),
            greetings_remaining: 5,
            max_history_length: 0,
        };
        let handle = client
            .start_workflow(
                RepeatGreetingWorkflow::run,
                input,
                WorkflowStartOptions::new(TASK_QUEUE, "no-hook").build(),
            )
            .await
            .unwrap();
        let result = handle.get_result(WorkflowGetResultOptions::default()).await.unwrap();
        assert_eq!(result, "Sent all greetings to Ziggy");

        // The start handle targets the first run in the chain.
        let status = handle.describe(WorkflowDescribeOptions::default()).await.unwrap().status();
        assert_eq!(status, WorkflowExecutionStatus::Completed);
    })
    .await;
}

//! Runs each "Cancellation" example against a local dev server.
//!
//! Set TEMPORAL_CLI_PATH to use an installed Temporal CLI instead of downloading one.

use std::time::Duration;

use cancellation::{activities::CancellationActivities, client, workflows::CancellationWorkflow};
use temporalio_client::{
    Client, UntypedWorkflow, WorkflowCancelOptions, WorkflowDescribeOptions,
    WorkflowExecutionStatus, WorkflowFetchHistoryOptions, WorkflowGetResultOptions,
    WorkflowStartOptions,
};
use temporalio_common::protos::temporal::api::enums::v1::EventType;
use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{
    Runtime, Worker, WorkerOptions, WorkflowContext, WorkflowResult, WorkflowTermination,
    testing::{EphemeralExe, LocalWorkflowEnvironmentOptions, WorkflowEnvironment},
};

const TASK_QUEUE: &str = "my-task-queue";

/// Test-only Workflow that propagates cancellation instead of handling it.
#[workflow]
#[derive(Default)]
pub struct PropagateCancellationWorkflow;

#[workflow_methods]
impl PropagateCancellationWorkflow {
    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<()> {
        ctx.cancelled().await;
        Err(WorkflowTermination::cancelled())
    }
}

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
        .register_activities(CancellationActivities)
        .register_workflow::<CancellationWorkflow>()
        .unwrap()
        .register_workflow::<PropagateCancellationWorkflow>()
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

async fn event_types(client: &Client, workflow_id: &str) -> Vec<EventType> {
    client
        .get_workflow_handle::<UntypedWorkflow>(workflow_id)
        .fetch_history(WorkflowFetchHistoryOptions::default())
        .into_events()
        .await
        .unwrap()
        .iter()
        .map(|e| e.event_type())
        .collect()
}

/// Polls until a Worker has started the Workflow's Activity and the Activity has heartbeated.
async fn wait_for_running_activity(client: &Client, workflow_id: &str) {
    let handle = client.get_workflow_handle::<UntypedWorkflow>(workflow_id);
    for _ in 0..100 {
        let description = handle.describe(WorkflowDescribeOptions::default()).await.unwrap();
        if description
            .raw()
            .pending_activities
            .iter()
            .any(|a| a.last_heartbeat_time.is_some())
        {
            return;
        }
        tokio::time::sleep(Duration::from_millis(100)).await;
    }
    panic!("{workflow_id} never started its Activity");
}

async fn status(client: &Client, workflow_id: &str) -> WorkflowExecutionStatus {
    client
        .get_workflow_handle::<UntypedWorkflow>(workflow_id)
        .describe(WorkflowDescribeOptions::default())
        .await
        .unwrap()
        .status()
}

#[tokio::test]
async fn cancel_running_activity_runs_cleanup() {
    with_worker(async |client| {
        let handle = client
            .start_workflow(
                CancellationWorkflow::run,
                (),
                WorkflowStartOptions::new(TASK_QUEUE, "cancel-running").build(),
            )
            .await
            .unwrap();
        wait_for_running_activity(&client, "cancel-running").await;
        handle
            .cancel(WorkflowCancelOptions::builder().reason("No longer needed").build())
            .await
            .unwrap();

        let result = handle.get_result(WorkflowGetResultOptions::default()).await.unwrap();
        assert_eq!(result, "Cancelled (reason=No longer needed), cleanup done");

        // The Workflow handled the cancellation and returned Ok, so it closes as Completed.
        assert_eq!(status(&client, "cancel-running").await, WorkflowExecutionStatus::Completed);

        let events = event_types(&client, "cancel-running").await;
        let position = |t: EventType| events.iter().position(|e| *e == t).unwrap();
        // A Workflow Task is scheduled to process the cancellation request.
        assert_eq!(
            events[position(EventType::WorkflowExecutionCancelRequested) + 1],
            EventType::WorkflowTaskScheduled
        );
        assert!(events.contains(&EventType::ActivityTaskCancelRequested));
        assert!(events.contains(&EventType::ActivityTaskCompleted));
    })
    .await;
}

#[tokio::test]
async fn client_cancel_example() {
    with_worker(async |client| {
        client::cancel_workflow(&client).await.unwrap();
        let result = client
            .get_workflow_handle::<CancellationWorkflow>("cancellation-workflow-1")
            .get_result(WorkflowGetResultOptions::default())
            .await
            .unwrap();
        assert_eq!(result, "Cancelled (reason=No longer needed), cleanup done");
    })
    .await;
}

#[tokio::test]
async fn propagating_cancellation_closes_as_canceled() {
    with_worker(async |client| {
        let handle = client
            .start_workflow(
                PropagateCancellationWorkflow::run,
                (),
                WorkflowStartOptions::new(TASK_QUEUE, "propagate").build(),
            )
            .await
            .unwrap();
        handle.cancel(WorkflowCancelOptions::default()).await.unwrap();
        assert!(handle.get_result(WorkflowGetResultOptions::default()).await.is_err());
        assert_eq!(status(&client, "propagate").await, WorkflowExecutionStatus::Canceled);
    })
    .await;
}

#[tokio::test]
async fn client_terminate_example() {
    with_worker(async |client| {
        client::terminate_workflow(&client).await.unwrap();
        assert_eq!(
            status(&client, "cancellation-workflow-2").await,
            WorkflowExecutionStatus::Terminated
        );
        let events = event_types(&client, "cancellation-workflow-2").await;
        // No Workflow Task is scheduled after termination.
        assert_eq!(events.last(), Some(&EventType::WorkflowExecutionTerminated));
    })
    .await;
}

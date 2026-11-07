//! Runs each "Child Workflows" example against a local dev server.
//!
//! Set TEMPORAL_CLI_PATH to use an installed Temporal CLI instead of downloading one.

use std::time::Duration;

use child_workflows::{
    parallel::{ComposeEnGreetingWorkflow, ComposeEsGreetingWorkflow, ParallelGreetingWorkflow},
    parent_close::AbandonGreetingWorkflow,
    start::{ComposeGreetingWorkflow, GreetingWorkflow},
};
use temporalio_client::{
    Client, UntypedWorkflow, WorkflowDescribeOptions, WorkflowExecutionStatus,
    WorkflowFetchHistoryOptions, WorkflowGetResultOptions, WorkflowStartOptions,
    WorkflowTerminateOptions,
};
use temporalio_common::protos::temporal::api::{
    enums::v1::{EventType, ParentClosePolicy as ProtoParentClosePolicy},
    history::v1::{HistoryEvent, history_event::Attributes},
};
use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{
    ChildWorkflowOptions, ParentClosePolicy, Runtime, Worker, WorkerOptions, WorkflowContext,
    WorkflowContextView, WorkflowResult,
    testing::{EphemeralExe, LocalWorkflowEnvironmentOptions, WorkflowEnvironment},
};

const TASK_QUEUE: &str = "child-workflows";
const LONG_CHILD_ID: &str = "long-running-child";

/// Test-only Child Workflow that runs until it's canceled or terminated.
#[workflow]
#[derive(Default)]
pub struct LongRunningChildWorkflow {
    done: bool,
}

#[workflow_methods]
impl LongRunningChildWorkflow {
    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<()> {
        ctx.wait_condition(|s| s.done).await?;
        Ok(())
    }
}

/// Test-only parent that starts the long-running child with a given policy and returns.
#[workflow]
pub struct PolicyParentWorkflow {
    policy: Option<String>,
}

#[workflow_methods]
impl PolicyParentWorkflow {
    #[init]
    fn new(_ctx: &WorkflowContextView, policy: Option<String>) -> Self {
        Self { policy }
    }

    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<()> {
        let policy = match ctx.state(|s| s.policy.clone()).as_deref() {
            None => ParentClosePolicy::default(),
            Some("Abandon") => ParentClosePolicy::Abandon,
            Some("RequestCancel") => ParentClosePolicy::RequestCancel,
            Some(other) => panic!("unknown policy {other}"),
        };
        ctx.start_child_workflow(
            LongRunningChildWorkflow::run,
            (),
            ChildWorkflowOptions::builder()
                .workflow_id(LONG_CHILD_ID.to_string())
                .parent_close_policy(policy)
                .build(),
        )
        .await?;
        Ok(())
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
        .register_workflow::<ComposeGreetingWorkflow>()
        .unwrap()
        .register_workflow::<GreetingWorkflow>()
        .unwrap()
        .register_workflow::<ComposeEnGreetingWorkflow>()
        .unwrap()
        .register_workflow::<ComposeEsGreetingWorkflow>()
        .unwrap()
        .register_workflow::<ParallelGreetingWorkflow>()
        .unwrap()
        .register_workflow::<AbandonGreetingWorkflow>()
        .unwrap()
        .register_workflow::<LongRunningChildWorkflow>()
        .unwrap()
        .register_workflow::<PolicyParentWorkflow>()
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

fn start_options(workflow_id: &str) -> WorkflowStartOptions {
    WorkflowStartOptions::new(TASK_QUEUE, workflow_id).build()
}

async fn history(client: &Client, workflow_id: &str) -> Vec<HistoryEvent> {
    client
        .get_workflow_handle::<UntypedWorkflow>(workflow_id)
        .fetch_history(WorkflowFetchHistoryOptions::default())
        .into_events()
        .await
        .unwrap()
}

/// Polls until the Workflow reaches `expected`, and returns its final status.
async fn wait_for_status(
    client: &Client,
    workflow_id: &str,
    expected: WorkflowExecutionStatus,
) -> WorkflowExecutionStatus {
    let handle = client.get_workflow_handle::<UntypedWorkflow>(workflow_id);
    let mut status = WorkflowExecutionStatus::Running;
    for _ in 0..50 {
        status = handle.describe(WorkflowDescribeOptions::default()).await.unwrap().status();
        if status == expected {
            break;
        }
        tokio::time::sleep(Duration::from_millis(100)).await;
    }
    status
}

/// Starts a PolicyParentWorkflow, waits for it to complete, and returns the child's status.
async fn child_status_after_parent_closes(
    client: &Client,
    policy: Option<&str>,
    expected: WorkflowExecutionStatus,
) -> WorkflowExecutionStatus {
    let handle = client
        .start_workflow(
            PolicyParentWorkflow::run,
            policy.map(str::to_string),
            start_options("policy-parent"),
        )
        .await
        .unwrap();
    handle.get_result(WorkflowGetResultOptions::default()).await.unwrap();
    wait_for_status(client, LONG_CHILD_ID, expected).await
}

#[tokio::test]
async fn start_returns_child_result() {
    with_worker(async |client| {
        let handle = client
            .start_workflow(GreetingWorkflow::run, "Ziggy".to_string(), start_options("start"))
            .await
            .unwrap();
        let result = handle.get_result(WorkflowGetResultOptions::default()).await.unwrap();
        assert_eq!(result, "ComposeGreetingWorkflow result: Hello from child: Ziggy");

        let events = history(&client, "start").await;
        assert!(events.iter().any(|e| e.event_type() == EventType::ChildWorkflowExecutionStarted));
    })
    .await;
}

#[tokio::test]
async fn parallel_returns_both_results() {
    with_worker(async |client| {
        let handle = client
            .start_workflow(
                ParallelGreetingWorkflow::run,
                "Ziggy".to_string(),
                start_options("parallel"),
            )
            .await
            .unwrap();
        let result = handle.get_result(WorkflowGetResultOptions::default()).await.unwrap();
        assert_eq!(result, vec![Some("Hello: Ziggy".to_string()), Some("Hola: Ziggy".to_string())]);

        // Both children start before the parent waits on either result.
        let events = history(&client, "parallel").await;
        let position = |t: EventType| events.iter().position(|e| e.event_type() == t).unwrap();
        let second_initiated = events
            .iter()
            .rposition(|e| e.event_type() == EventType::StartChildWorkflowExecutionInitiated)
            .unwrap();
        assert!(second_initiated < position(EventType::ChildWorkflowExecutionCompleted));
    })
    .await;
}

#[tokio::test]
async fn parent_close_policy_is_sent_with_start() {
    with_worker(async |client| {
        let handle = client
            .start_workflow(
                AbandonGreetingWorkflow::run,
                "Ziggy".to_string(),
                start_options("abandon"),
            )
            .await
            .unwrap();
        let result = handle.get_result(WorkflowGetResultOptions::default()).await.unwrap();
        assert_eq!(result, "Hola: Ziggy");

        let events = history(&client, "abandon").await;
        let policy = events
            .iter()
            .find_map(|e| match &e.attributes {
                Some(Attributes::StartChildWorkflowExecutionInitiatedEventAttributes(a)) => {
                    Some(a.parent_close_policy())
                }
                _ => None,
            })
            .unwrap();
        assert_eq!(policy, ProtoParentClosePolicy::Abandon);
    })
    .await;
}

#[tokio::test]
async fn awaiting_start_waits_for_child_started_event() {
    with_worker(async |client| {
        child_status_after_parent_closes(&client, Some("Abandon"), WorkflowExecutionStatus::Running)
            .await;
        let events = history(&client, "policy-parent").await;
        let position = |t: EventType| events.iter().position(|e| e.event_type() == t).unwrap();
        assert!(
            position(EventType::ChildWorkflowExecutionStarted)
                < position(EventType::WorkflowExecutionCompleted)
        );
        client
            .get_workflow_handle::<UntypedWorkflow>(LONG_CHILD_ID)
            .terminate(WorkflowTerminateOptions::default())
            .await
            .unwrap();
    })
    .await;
}

#[tokio::test]
async fn default_policy_terminates_child() {
    with_worker(async |client| {
        let status =
            child_status_after_parent_closes(&client, None, WorkflowExecutionStatus::Terminated)
                .await;
        assert_eq!(status, WorkflowExecutionStatus::Terminated);
    })
    .await;
}

#[tokio::test]
async fn abandon_policy_leaves_child_running() {
    with_worker(async |client| {
        let status = child_status_after_parent_closes(
            &client,
            Some("Abandon"),
            WorkflowExecutionStatus::Running,
        )
        .await;
        // Give the server time to apply a policy if it were going to.
        tokio::time::sleep(Duration::from_secs(1)).await;
        let status_later = wait_for_status(&client, LONG_CHILD_ID, WorkflowExecutionStatus::Running)
            .await;
        assert_eq!(status, WorkflowExecutionStatus::Running);
        assert_eq!(status_later, WorkflowExecutionStatus::Running);
        client
            .get_workflow_handle::<UntypedWorkflow>(LONG_CHILD_ID)
            .terminate(WorkflowTerminateOptions::default())
            .await
            .unwrap();
    })
    .await;
}

#[tokio::test]
async fn request_cancel_policy_cancels_child() {
    with_worker(async |client| {
        let status = child_status_after_parent_closes(
            &client,
            Some("RequestCancel"),
            WorkflowExecutionStatus::Canceled,
        )
        .await;
        assert_eq!(status, WorkflowExecutionStatus::Canceled);
    })
    .await;
}

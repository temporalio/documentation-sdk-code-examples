//! Runs the "Timers" example against a local dev server.
//!
//! Set TEMPORAL_CLI_PATH to use an installed Temporal CLI instead of downloading one.

use std::time::Duration;

use temporalio_client::{
    Client, UntypedWorkflow, WorkflowFetchHistoryOptions, WorkflowGetResultOptions,
    WorkflowStartOptions, WorkflowTerminateOptions,
};
use temporalio_common::protos::temporal::api::{
    enums::v1::EventType,
    history::v1::{HistoryEvent, history_event::Attributes},
};
use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{
    Runtime, Worker, WorkerOptions, WorkflowContext, WorkflowResult,
    testing::{EphemeralExe, LocalServer, LocalWorkflowEnvironmentOptions, WorkflowEnvironment},
};
use timers::workflows::TimerWorkflow;

const TASK_QUEUE: &str = "timers";

/// Test-only Workflow with a short Timer, so the test can wait for it to fire.
#[workflow]
#[derive(Default)]
pub struct ShortTimerWorkflow;

#[workflow_methods]
impl ShortTimerWorkflow {
    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        ctx.timer(Duration::from_secs(2)).await;
        Ok("Timer fired".to_string())
    }
}

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
        .register_workflow::<TimerWorkflow>()
        .unwrap()
        .register_workflow::<ShortTimerWorkflow>()
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

/// Polls history until it contains a TimerStarted event, and returns that event.
async fn wait_for_timer_started(client: &Client, workflow_id: &str) -> HistoryEvent {
    let handle = client.get_workflow_handle::<UntypedWorkflow>(workflow_id);
    for _ in 0..100 {
        let events = handle
            .fetch_history(WorkflowFetchHistoryOptions::default())
            .into_events()
            .await
            .unwrap();
        if let Some(event) = events.into_iter().find(|e| e.event_type() == EventType::TimerStarted) {
            return event;
        }
        tokio::time::sleep(Duration::from_millis(100)).await;
    }
    panic!("{workflow_id} never started a Timer");
}

#[tokio::test]
async fn timer_records_duration_and_summary() {
    let env = start_env().await;
    let client = env.client().clone();
    with_worker(&client, async || {
        let handle = client
            .start_workflow(
                TimerWorkflow::run,
                (),
                WorkflowStartOptions::new(TASK_QUEUE, "timer").build(),
            )
            .await
            .unwrap();
        let event = wait_for_timer_started(&client, "timer").await;

        let Some(Attributes::TimerStartedEventAttributes(attributes)) = &event.attributes else {
            panic!("unexpected attributes: {:?}", event.attributes);
        };
        assert_eq!(attributes.start_to_fire_timeout.as_ref().unwrap().seconds, 60);

        let summary = event.user_metadata.as_ref().unwrap().summary.as_ref().unwrap();
        assert_eq!(summary.data, b"\"important timer\"");

        handle.terminate(WorkflowTerminateOptions::default()).await.unwrap();
    })
    .await;
    env.shutdown().await.unwrap();
}

#[tokio::test]
async fn timer_fires_after_worker_downtime() {
    let env = start_env().await;
    let client = env.client().clone();

    let handle = client
        .start_workflow(
            ShortTimerWorkflow::run,
            (),
            WorkflowStartOptions::new(TASK_QUEUE, "downtime").build(),
        )
        .await
        .unwrap();

    // The first Worker starts the Timer, then shuts down before it fires.
    with_worker(&client, async || {
        wait_for_timer_started(&client, "downtime").await;
    })
    .await;
    tokio::time::sleep(Duration::from_secs(3)).await;

    // A new Worker picks up the fired Timer and finishes the Workflow.
    let result = with_worker(&client, async || {
        handle.get_result(WorkflowGetResultOptions::default()).await.unwrap()
    })
    .await;
    assert_eq!(result, "Timer fired");

    let events = client
        .get_workflow_handle::<UntypedWorkflow>("downtime")
        .fetch_history(WorkflowFetchHistoryOptions::default())
        .into_events()
        .await
        .unwrap();
    assert!(events.iter().any(|e| e.event_type() == EventType::TimerFired));
    env.shutdown().await.unwrap();
}

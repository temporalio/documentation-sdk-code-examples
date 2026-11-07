//! Runs each "Workflow basics" example against a local dev server.
//!
//! Set TEMPORAL_CLI_PATH to use an installed Temporal CLI instead of downloading one.

use std::time::Duration;

use temporalio_client::{
    Client, WorkflowDescribeOptions, WorkflowExecuteUpdateOptions, WorkflowExecutionStatus,
    WorkflowFetchHistoryOptions, WorkflowGetResultOptions, WorkflowSignalOptions,
    WorkflowStartOptions, WorkflowTerminateOptions, errors::WorkflowGetResultError,
};
use temporalio_common::protos::temporal::api::enums::v1::EventType;
use temporalio_sdk::{
    Runtime, Worker, WorkerOptions,
    testing::{EphemeralExe, LocalWorkflowEnvironmentOptions, WorkflowEnvironment},
};
use workflow_basics::{
    activities::MyActivities,
    constructor::{MyWorkflowInput, WorkflowRunSeesWorkflowInitWorkflow},
    custom_type, definition,
    determinism::DeterministicWorkflow,
    failure::ValidationWorkflow,
    init::{PersonInput, PersonWorkflow},
    parameters::{ProcessingInput, ProcessingWorkflow},
    return_values::{ProcessingResult, ReturnValueWorkflow},
    run_method::RunMethodWorkflow,
    state::StateWorkflow,
};

const TASK_QUEUE: &str = "workflow-basics";

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
        .register_workflow::<definition::GreetingWorkflow>()
        .unwrap()
        .register_workflow::<PersonWorkflow>()
        .unwrap()
        .register_workflow::<WorkflowRunSeesWorkflowInitWorkflow>()
        .unwrap()
        .register_workflow::<RunMethodWorkflow>()
        .unwrap()
        .register_workflow::<ProcessingWorkflow>()
        .unwrap()
        .register_workflow::<ReturnValueWorkflow>()
        .unwrap()
        .register_workflow::<custom_type::GreetingWorkflow>()
        .unwrap()
        .register_workflow::<DeterministicWorkflow>()
        .unwrap()
        .register_workflow::<StateWorkflow>()
        .unwrap()
        .register_workflow::<ValidationWorkflow>()
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

#[tokio::test]
async fn definition_returns_greeting_and_uses_struct_name_as_type() {
    with_worker(async |client| {
        let handle = client
            .start_workflow(
                definition::GreetingWorkflow::run,
                "Temporal".to_string(),
                start_options("definition"),
            )
            .await
            .unwrap();
        let result = handle.get_result(WorkflowGetResultOptions::default()).await.unwrap();
        assert_eq!(result, "Hello, Temporal!");

        let description = handle.describe(WorkflowDescribeOptions::default()).await.unwrap();
        assert_eq!(description.workflow_type(), "GreetingWorkflow");
    })
    .await;
}

#[tokio::test]
async fn init_reads_input_and_start_time() {
    with_worker(async |client| {
        let input = PersonInput { name: "Ziggy".to_string(), age: 42 };
        let handle = client
            .start_workflow(PersonWorkflow::run, input, start_options("init"))
            .await
            .unwrap();
        let result = handle.get_result(WorkflowGetResultOptions::default()).await.unwrap();
        assert_eq!(result, "Ziggy is 42; start time recorded: true");
    })
    .await;
}

#[tokio::test]
async fn constructor_state_is_visible_to_update_and_run() {
    with_worker(async |client| {
        let input = MyWorkflowInput { name: "Ziggy".to_string() };
        let handle = client
            .start_workflow(
                WorkflowRunSeesWorkflowInitWorkflow::get_greeting,
                input,
                start_options("constructor"),
            )
            .await
            .unwrap();
        let title_is_valid = handle
            .execute_update(
                WorkflowRunSeesWorkflowInitWorkflow::check_title_validity,
                (),
                WorkflowExecuteUpdateOptions::default(),
            )
            .await
            .unwrap();
        assert!(title_is_valid);

        let result = handle.get_result(WorkflowGetResultOptions::default()).await.unwrap();
        assert_eq!(result, "Hello, Knight Ziggy");
    })
    .await;
}

#[tokio::test]
async fn run_method_executes_activity() {
    with_worker(async |client| {
        let handle = client
            .start_workflow(RunMethodWorkflow::run, "Ziggy".to_string(), start_options("run-method"))
            .await
            .unwrap();
        let result = handle.get_result(WorkflowGetResultOptions::default()).await.unwrap();
        assert_eq!(result, "Hello, Ziggy!");
    })
    .await;
}

#[tokio::test]
async fn parameters_accept_input_struct() {
    with_worker(async |client| {
        let input = ProcessingInput {
            data: vec!["a".to_string(), "b".to_string()],
            timeout_seconds: 30,
        };
        let handle = client
            .start_workflow(ProcessingWorkflow::run, input, start_options("parameters"))
            .await
            .unwrap();
        let result = handle.get_result(WorkflowGetResultOptions::default()).await.unwrap();
        assert_eq!(result, "Processing complete");
    })
    .await;
}

#[tokio::test]
async fn return_values_deserialize_struct() {
    with_worker(async |client| {
        let handle = client
            .start_workflow(ReturnValueWorkflow::run, (), start_options("return-values"))
            .await
            .unwrap();
        let result = handle.get_result(WorkflowGetResultOptions::default()).await.unwrap();
        assert_eq!(
            result,
            ProcessingResult { status: "completed".to_string(), records_processed: 100 }
        );
    })
    .await;
}

#[tokio::test]
async fn custom_type_sets_workflow_type_name() {
    with_worker(async |client| {
        let handle = client
            .start_workflow(
                custom_type::GreetingWorkflow::run,
                "Ziggy".to_string(),
                start_options("custom-type"),
            )
            .await
            .unwrap();
        let result = handle.get_result(WorkflowGetResultOptions::default()).await.unwrap();
        assert_eq!(result, "Hello, Ziggy!");

        let description = handle.describe(WorkflowDescribeOptions::default()).await.unwrap();
        assert_eq!(description.workflow_type(), "my-custom-workflow");
    })
    .await;
}

#[tokio::test]
async fn determinism_starts_durable_timer() {
    with_worker(async |client| {
        let handle = client
            .start_workflow(DeterministicWorkflow::run, (), start_options("determinism"))
            .await
            .unwrap();
        for item in ["a", "b", "c"] {
            handle
                .signal(
                    DeterministicWorkflow::add_data,
                    item.to_string(),
                    WorkflowSignalOptions::default(),
                )
                .await
                .unwrap();
        }

        // The 60-second Timer is recorded in Event History, so the Workflow waits on it.
        let mut timer_started = false;
        for _ in 0..50 {
            let events = handle
                .fetch_history(WorkflowFetchHistoryOptions::default())
                .into_events()
                .await
                .unwrap();
            if events.iter().any(|e| e.event_type() == EventType::TimerStarted) {
                timer_started = true;
                break;
            }
            tokio::time::sleep(Duration::from_millis(100)).await;
        }
        assert!(timer_started);

        let description = handle.describe(WorkflowDescribeOptions::default()).await.unwrap();
        assert_eq!(description.status(), WorkflowExecutionStatus::Running);
        handle.terminate(WorkflowTerminateOptions::default()).await.unwrap();
    })
    .await;
}

#[tokio::test]
async fn state_supports_read_and_write() {
    with_worker(async |client| {
        let handle = client
            .start_workflow(StateWorkflow::run, "Ziggy".to_string(), start_options("state"))
            .await
            .unwrap();
        let result = handle.get_result(WorkflowGetResultOptions::default()).await.unwrap();
        assert_eq!(result, "Ziggy (greeted)");
    })
    .await;
}

#[tokio::test]
async fn failure_fails_workflow_execution() {
    with_worker(async |client| {
        let handle = client
            .start_workflow(ValidationWorkflow::run, String::new(), start_options("failure-empty"))
            .await
            .unwrap();
        let error = handle.get_result(WorkflowGetResultOptions::default()).await.unwrap_err();
        assert!(matches!(error, WorkflowGetResultError::Failed(_)));
        assert!(error.to_string().contains("validation_failed"), "{error}");

        let handle = client
            .start_workflow(ValidationWorkflow::run, "data".to_string(), start_options("failure-ok"))
            .await
            .unwrap();
        let result = handle.get_result(WorkflowGetResultOptions::default()).await.unwrap();
        assert_eq!(result, "Success");
    })
    .await;
}

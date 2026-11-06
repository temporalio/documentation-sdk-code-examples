//! Runs each "Workflow message passing" example against a local dev server.
//!
//! Set TEMPORAL_CLI_PATH to use an installed Temporal CLI instead of downloading one.

use message_passing::{
    activities::GreetingActivities,
    approver::ApproverWorkflow,
    client,
    workflows::{ApproveInput, GetLanguagesInput, GreetingsWorkflow, Language, SetLanguageInput},
};
use temporalio_client::{
    Client, RpcOptions, UntypedWorkflow, WorkflowExecuteUpdateOptions,
    WorkflowFetchHistoryOptions, WorkflowGetResultOptions, WorkflowQueryOptions,
    WorkflowSignalOptions, WorkflowStartOptions, WorkflowStartUpdateOptions,
    WorkflowTerminateOptions,
};
use temporalio_common::protos::temporal::api::enums::v1::EventType;
use temporalio_sdk::{
    Runtime, Worker, WorkerOptions,
    testing::{EphemeralExe, LocalWorkflowEnvironmentOptions, WorkflowEnvironment},
};

const TASK_QUEUE: &str = "my-task-queue";

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
        .register_activities(GreetingActivities)
        .register_workflow::<GreetingsWorkflow>()
        .unwrap()
        .register_workflow::<ApproverWorkflow>()
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

#[tokio::test]
async fn client_sends_query_updates_and_signal() {
    with_worker(async |client| {
        let results = client::send_messages(&client).await.unwrap();
        assert_eq!(results.supported_languages.len(), 6);
        assert_eq!(results.previous_language, Language::English);
        assert_eq!(results.previous_language_from_activity_update, Language::Chinese);
        assert_eq!(results.greeting, "Bonjour, monde");
    })
    .await;
}

#[tokio::test]
async fn validator_rejects_update_before_history() {
    with_worker(async |client| {
        let handle = client
            .start_workflow(GreetingsWorkflow::run, (), start_options("validator"))
            .await
            .unwrap();
        let error = handle
            .execute_update(
                GreetingsWorkflow::set_language,
                SetLanguageInput { language: Language::French },
                WorkflowExecuteUpdateOptions::default(),
            )
            .await
            .unwrap_err();
        assert!(error.to_string().contains("Not a valid language") || format!("{error:?}").contains("Not a valid language"), "{error:?}");

        let events = event_types(&client, "validator").await;
        assert!(!events.contains(&EventType::WorkflowExecutionUpdateAccepted));
        handle.terminate(WorkflowTerminateOptions::default()).await.unwrap();
    })
    .await;
}

#[tokio::test]
async fn query_adds_no_events_and_works_after_close() {
    with_worker(async |client| {
        let handle = client
            .start_workflow(GreetingsWorkflow::run, (), start_options("query"))
            .await
            .unwrap();
        let query = async || {
            handle
                .query(
                    GreetingsWorkflow::get_languages,
                    GetLanguagesInput { include_unsupported: false },
                    WorkflowQueryOptions::default(),
                )
                .await
                .unwrap()
        };
        // The first Query runs after the first Workflow Task, so history is settled.
        assert_eq!(query().await, vec![Language::Chinese, Language::English]);
        let before = event_types(&client, "query").await;
        assert_eq!(query().await, vec![Language::Chinese, Language::English]);
        assert_eq!(event_types(&client, "query").await, before);

        handle
            .signal(
                GreetingsWorkflow::approve,
                ApproveInput { name: "Ziggy".to_string() },
                WorkflowSignalOptions::default(),
            )
            .await
            .unwrap();
        handle.get_result(WorkflowGetResultOptions::default()).await.unwrap();
        assert_eq!(query().await, vec![Language::Chinese, Language::English]);
    })
    .await;
}

#[tokio::test]
async fn run_waits_for_async_update_handler() {
    with_worker(async |client| {
        let handle = client
            .start_workflow(GreetingsWorkflow::run, (), start_options("handlers-finish"))
            .await
            .unwrap();
        let update_handle = handle
            .start_update(
                GreetingsWorkflow::set_language_activity,
                SetLanguageInput { language: Language::Spanish },
                WorkflowStartUpdateOptions::default(),
            )
            .await
            .unwrap();
        // Approve while the Update's Activity is still running.
        handle
            .signal(
                GreetingsWorkflow::approve,
                ApproveInput { name: "Ziggy".to_string() },
                WorkflowSignalOptions::default(),
            )
            .await
            .unwrap();
        let greeting = handle.get_result(WorkflowGetResultOptions::default()).await.unwrap();
        assert_eq!(greeting, "Hola mundo");
        assert_eq!(update_handle.get_result(RpcOptions::default()).await.unwrap(), Language::English);
    })
    .await;
}

#[tokio::test]
async fn signal_with_start_delivers_signal() {
    with_worker(async |client| {
        let greeting = client::signal_with_start(&client).await.unwrap();
        assert_eq!(greeting, "Hello, world");
    })
    .await;
}

#[tokio::test]
async fn update_with_start_uses_existing_workflow() {
    with_worker(async |client| {
        assert_eq!(client::update_with_start(&client).await.unwrap(), Language::English);
        // The second call attaches to the same running Workflow.
        assert_eq!(client::update_with_start(&client).await.unwrap(), Language::Chinese);
        client
            .get_workflow_handle::<UntypedWorkflow>("greetings-workflow-12")
            .terminate(WorkflowTerminateOptions::default())
            .await
            .unwrap();
    })
    .await;
}

#[tokio::test]
async fn workflow_signals_another_workflow() {
    with_worker(async |client| {
        let handle = client
            .start_workflow(GreetingsWorkflow::run, (), start_options("signal-target"))
            .await
            .unwrap();
        let approver = client
            .start_workflow(
                ApproverWorkflow::run,
                "signal-target".to_string(),
                start_options("approver"),
            )
            .await
            .unwrap();
        approver.get_result(WorkflowGetResultOptions::default()).await.unwrap();
        let greeting = handle.get_result(WorkflowGetResultOptions::default()).await.unwrap();
        assert_eq!(greeting, "Hello, world");
    })
    .await;
}

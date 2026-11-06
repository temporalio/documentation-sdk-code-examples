//! Runs the Standalone Activity from the quickstart against a local dev server.
//!
//! Set TEMPORAL_CLI_PATH to use an installed Temporal CLI instead of downloading one.

use std::time::Duration;

use standalone_activities_quickstart::activities::GreetingActivities;
use temporalio_client::ActivityStartOptions;
use temporalio_sdk::{
    Runtime, Worker, WorkerOptions,
    testing::{EphemeralExe, LocalWorkflowEnvironmentOptions, WorkflowEnvironment},
};

const TASK_QUEUE: &str = "standalone-activities";

#[tokio::test]
async fn standalone_activity_returns_greeting() {
    let mut options = LocalWorkflowEnvironmentOptions::default();
    if let Ok(path) = std::env::var("TEMPORAL_CLI_PATH") {
        options.server_executable = EphemeralExe::ExistingPath(path);
    }
    let env = WorkflowEnvironment::start_local(options).await.unwrap();
    let client = env.client().clone();

    let runtime = Runtime::from_current_tokio(Default::default()).unwrap();
    let worker_options = WorkerOptions::new(TASK_QUEUE)
        .register_activities(GreetingActivities)
        .build();
    let mut worker = Worker::new(&runtime, client.clone(), worker_options).unwrap();
    let shutdown = worker.shutdown_handle();

    let (result, run) = tokio::join!(
        async {
            let handle = client
                .start_activity(
                    GreetingActivities::compose_greeting,
                    ("Hello".to_string(), "Temporal".to_string()),
                    ActivityStartOptions::with_start_to_close_timeout(
                        TASK_QUEUE,
                        "standalone-activity-id",
                        Duration::from_secs(10),
                    )
                    .build(),
                )
                .await
                .unwrap();
            let result = handle.result().await.unwrap();
            shutdown();
            result
        },
        worker.run()
    );
    run.unwrap();
    assert_eq!(result, "Hello, Temporal!");
    env.shutdown().await.unwrap();
}

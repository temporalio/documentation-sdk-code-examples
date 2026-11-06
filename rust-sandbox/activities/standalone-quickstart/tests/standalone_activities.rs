//! Runs the Standalone Activities feature guide examples against a local dev server.
//!
//! Set TEMPORAL_CLI_PATH to use an installed Temporal CLI instead of downloading one.

use std::time::Duration;

use futures::StreamExt;
use standalone_activities_quickstart::activities::GreetingActivities;
use temporalio_client::{
    ActivityDescribeOptions, ActivityExecutionInfoLike, ActivityStartOptions, Client,
};
use temporalio_sdk::{
    Runtime, Worker, WorkerOptions,
    testing::{EphemeralExe, LocalServer, LocalWorkflowEnvironmentOptions, WorkflowEnvironment},
};

const TASK_QUEUE: &str = "standalone-activities";
const QUERY: &str = "TaskQueue = 'standalone-activities'";

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
        .register_activities(GreetingActivities)
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

fn start_options(activity_id: &str) -> ActivityStartOptions {
    ActivityStartOptions::with_start_to_close_timeout(
        TASK_QUEUE,
        activity_id,
        Duration::from_secs(10),
    )
    .build()
}

/// Polls `count_activities` until visibility reports `expected` Activities.
async fn wait_for_count(client: &Client, expected: usize) {
    for _ in 0..100 {
        let count = client
            .count_activities(QUERY, Default::default())
            .await
            .unwrap();
        if count.count() == expected {
            return;
        }
        tokio::time::sleep(Duration::from_millis(100)).await;
    }
    panic!("visibility never reported {expected} Activities");
}

#[tokio::test]
async fn start_get_handle_describe_list_count() {
    let env = start_env().await;
    let client = env.client().clone();
    with_worker(&client, async || {
        let handle = client
            .start_activity(
                GreetingActivities::compose_greeting,
                ("Hello".to_string(), "Temporal".to_string()),
                start_options("standalone-activity-id"),
            )
            .await
            .unwrap();
        assert_eq!(handle.activity_id(), "standalone-activity-id");
        assert!(handle.run_id().is_some());

        let handle = client.get_activity_handle(
            GreetingActivities::compose_greeting,
            "standalone-activity-id",
            None,
        );
        assert_eq!(handle.result().await.unwrap(), "Hello, Temporal!");

        let description = handle
            .describe(
                ActivityDescribeOptions::builder()
                    .include_outcome(true)
                    .build(),
            )
            .await
            .unwrap();
        assert_eq!(
            description.activity_type(),
            "GreetingActivities::compose_greeting"
        );
        assert_eq!(description.attempt(), 1);
        assert!(format!("{:?}", description.status()).contains("Completed"));

        wait_for_count(&client, 1).await;
        let executions: Vec<_> = client
            .list_activities(QUERY, Default::default())
            .collect()
            .await;
        assert_eq!(executions.len(), 1);
        let execution = executions.into_iter().next().unwrap().unwrap();
        assert_eq!(execution.activity_id(), "standalone-activity-id");
    })
    .await;
    env.shutdown().await.unwrap();
}

/// Starts an Activity that no Worker picks up, so it stays scheduled.
async fn start_pending(client: &Client, activity_id: &str) {
    client
        .start_activity(
            GreetingActivities::compose_greeting,
            ("Hello".to_string(), "Temporal".to_string()),
            start_options(activity_id),
        )
        .await
        .unwrap();
}

#[tokio::test]
async fn cancel_and_terminate_pending_activities() {
    let env = start_env().await;
    let client = env.client().clone();

    // Canceling an Activity that hasn't started closes it as Canceled right away.
    start_pending(&client, "cancel-me").await;
    let handle =
        client.get_activity_handle(GreetingActivities::compose_greeting, "cancel-me", None);
    handle.cancel(Default::default()).await.unwrap();
    let description = handle.describe(Default::default()).await.unwrap();
    assert!(format!("{:?}", description.status()).contains("Canceled"));
    assert!(handle.result().await.is_err());

    start_pending(&client, "terminate-me").await;
    let handle =
        client.get_activity_handle(GreetingActivities::compose_greeting, "terminate-me", None);
    handle.terminate(Default::default()).await.unwrap();
    let description = handle.describe(Default::default()).await.unwrap();
    assert!(format!("{:?}", description.status()).contains("Terminated"));
    assert!(handle.result().await.is_err());

    env.shutdown().await.unwrap();
}

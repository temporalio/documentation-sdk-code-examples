//! Runs the "Activity Timeouts" examples.
//!
//! The Workflow test starts a local dev server. Set TEMPORAL_CLI_PATH to use an installed
//! Temporal CLI instead of downloading one.

use std::sync::{Arc, Mutex};
use std::time::Duration;

use activity_timeouts::{
    activities::MyActivities, heartbeat::HeartbeatActivities, retry_delay::TestGreetActivities,
    workflows::ActivityTimeoutsWorkflow,
};
use temporalio_client::{
    UntypedWorkflow, WorkflowFetchHistoryOptions, WorkflowGetResultOptions, WorkflowStartOptions,
};
use temporalio_common::protos::temporal::api::history::v1::history_event::Attributes;
use temporalio_sdk::{
    Runtime, Worker, WorkerOptions,
    activities::ActivityError,
    testing::{
        ActivityEnvironment, ActivityEnvironmentError, EphemeralExe,
        LocalWorkflowEnvironmentOptions, TestActivityInfoOptions, WorkflowEnvironment,
    },
};

const TASK_QUEUE: &str = "activity-timeouts";

#[tokio::test]
async fn workflow_schedules_activities_with_timeouts_and_retry_policy() {
    let mut options = LocalWorkflowEnvironmentOptions::default();
    if let Ok(path) = std::env::var("TEMPORAL_CLI_PATH") {
        options.server_executable = EphemeralExe::ExistingPath(path);
    }
    let env = WorkflowEnvironment::start_local(options).await.unwrap();
    let client = env.client().clone();

    let runtime = Runtime::from_current_tokio(Default::default()).unwrap();
    let worker_options = WorkerOptions::new(TASK_QUEUE)
        .register_workflow::<ActivityTimeoutsWorkflow>()
        .unwrap()
        .register_activities(MyActivities)
        .build();
    let mut worker = Worker::new(&runtime, client.clone(), worker_options).unwrap();
    let shutdown = worker.shutdown_handle();

    let (result, run) = tokio::join!(
        async {
            let handle = client
                .start_workflow(
                    ActivityTimeoutsWorkflow::run,
                    "Temporal".to_string(),
                    WorkflowStartOptions::new(TASK_QUEUE, "activity-timeouts").build(),
                )
                .await
                .unwrap();
            let result = handle
                .get_result(WorkflowGetResultOptions::default())
                .await
                .unwrap();
            shutdown();
            result
        },
        worker.run()
    );
    run.unwrap();
    assert_eq!(result, "Hello, Temporal! (English, English)");

    let scheduled: Vec<_> = client
        .get_workflow_handle::<UntypedWorkflow>("activity-timeouts")
        .fetch_history(WorkflowFetchHistoryOptions::default())
        .into_events()
        .await
        .unwrap()
        .into_iter()
        .filter_map(|event| match event.attributes {
            Some(Attributes::ActivityTaskScheduledEventAttributes(attributes)) => Some(attributes),
            _ => None,
        })
        .collect();
    assert_eq!(scheduled.len(), 3);

    assert_eq!(scheduled[0].start_to_close_timeout.unwrap().seconds, 30);

    let policy = scheduled[1].retry_policy.clone().unwrap();
    assert_eq!(policy.initial_interval.unwrap().seconds, 10);
    assert_eq!(policy.backoff_coefficient, 2.0);
    assert_eq!(policy.maximum_interval.unwrap().seconds, 100);
    assert_eq!(policy.maximum_attempts, 5);
    assert_eq!(policy.non_retryable_error_types, vec!["NonRetryableError"]);

    assert_eq!(scheduled[2].heartbeat_timeout.unwrap().seconds, 5);

    env.shutdown().await.unwrap();
}

/// Runs `TestGreetActivities::greet` as the given attempt.
async fn greet_attempt(name: &str, attempt: u32) -> Result<String, ActivityEnvironmentError> {
    ActivityEnvironment::builder_with_default()
        .info(TestActivityInfoOptions::builder().attempt(attempt).build())
        .build()
        .run(TestGreetActivities::greet, name.to_string())
        .await
}

#[tokio::test]
async fn next_retry_delay_grows_with_each_attempt() {
    assert_eq!(
        greet_attempt("Temporal", 1).await.unwrap(),
        "Hello, Temporal!"
    );

    for (attempt, seconds) in [(1, 5), (2, 10), (3, 15)] {
        let Err(ActivityEnvironmentError::Activity(ActivityError::Application(failure))) =
            greet_attempt("ziggy", attempt).await
        else {
            panic!("expected an application failure");
        };
        assert!(!failure.is_non_retryable());
        assert_eq!(
            failure.next_retry_delay(),
            Some(Duration::from_secs(seconds))
        );
    }
}

#[tokio::test]
async fn greet_records_a_heartbeat() {
    let heartbeats = Arc::new(Mutex::new(Vec::new()));
    let recorded = heartbeats.clone();
    let env = ActivityEnvironment::builder_with_default()
        .on_heartbeat(move |details| {
            let details = details.downcast::<String>().unwrap();
            recorded.lock().unwrap().push(*details);
        })
        .build();

    let greeting = env
        .run(HeartbeatActivities::greet, "Temporal".to_string())
        .await
        .unwrap();
    assert_eq!(greeting, "Hello, Temporal!");
    assert_eq!(
        *heartbeats.lock().unwrap(),
        vec!["greet activity started".to_string()]
    );

    let Err(ActivityEnvironmentError::Activity(ActivityError::Application(failure))) = env
        .run(HeartbeatActivities::greet, "ziggy".to_string())
        .await
    else {
        panic!("expected an application failure");
    };
    assert!(!failure.is_non_retryable());
}

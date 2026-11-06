//! Runs the "Activity basics" examples in a test Activity environment.

use std::sync::Arc;
use std::time::Duration;

use activity_basics::{activity_type, arc_instance, definition, parameters, return_values};
use temporalio_sdk::{
    activities::ActivityError,
    testing::{ActivityEnvironment, ActivityEnvironmentError},
};
use tokio::sync::Semaphore;

fn env() -> ActivityEnvironment {
    ActivityEnvironment::builder_with_default().build()
}

/// Returns the ApplicationFailure inside an Activity error.
fn application_failure(
    err: ActivityEnvironmentError,
) -> Box<temporalio_common::error::ApplicationFailure> {
    match err {
        ActivityEnvironmentError::Activity(ActivityError::Application(failure)) => failure,
        other => panic!("expected an application failure, got {other:?}"),
    }
}

#[tokio::test]
async fn basic_activities_run() {
    let env = env();
    let greeting = env
        .run(definition::GreetingActivities::greet, "World".to_string())
        .await
        .unwrap();
    assert_eq!(greeting, "Hello, World!");

    env.run(
        definition::GreetingActivities::send_notification,
        "Order shipped".to_string(),
    )
    .await
    .unwrap();
}

#[tokio::test]
async fn instance_activity_waits_for_cancellation() {
    let acts_started = Arc::new(Semaphore::new(0));
    let acts_done = Arc::new(Semaphore::new(0));
    let env = ActivityEnvironment::builder_with_default()
        .register_activities(arc_instance::SleeperActivities {
            acts_started: acts_started.clone(),
            acts_done: acts_done.clone(),
        })
        .build();

    let (result, _) = tokio::join!(
        env.run(arc_instance::SleeperActivities::sleeper, String::new()),
        async {
            acts_started.acquire().await.unwrap().forget();
            env.cancel();
        }
    );

    assert!(matches!(
        result,
        Err(ActivityEnvironmentError::Activity(
            ActivityError::Cancelled { .. }
        ))
    ));
    assert_eq!(acts_done.available_permits(), 1);
}

#[tokio::test]
async fn struct_parameter_is_used() {
    let greeting = env()
        .run(
            parameters::GreetingActivities::compose_greeting,
            parameters::GreetingInput {
                greeting: "Hello".to_string(),
                name: "Temporal".to_string(),
            },
        )
        .await
        .unwrap();
    assert_eq!(greeting, "Hello Temporal!");
}

#[tokio::test]
async fn process_data_returns_struct_or_failure() {
    let env = env();
    let data = env
        .run(return_values::MyActivities::process_data, "abc".to_string())
        .await
        .unwrap();
    assert_eq!(data.processed, "ABC");

    let retryable = application_failure(
        env.run(return_values::MyActivities::process_data, String::new())
            .await
            .unwrap_err(),
    );
    assert!(!retryable.is_non_retryable());
    assert_eq!(retryable.next_retry_delay(), Some(Duration::from_secs(5)));

    let permanent = application_failure(
        env.run(
            return_values::MyActivities::process_data,
            "x".repeat(1_000_001),
        )
        .await
        .unwrap_err(),
    );
    assert!(permanent.is_non_retryable());
}

#[test]
fn custom_activity_type_names() {
    assert_eq!(
        activity_type::GreetingActivities::greet.name(),
        "compose_greeting"
    );
    assert_eq!(
        activity_type::GreetingActivities::send_notification.name(),
        "send_email"
    );
    assert_eq!(
        definition::GreetingActivities::greet.name(),
        "GreetingActivities::greet"
    );
}

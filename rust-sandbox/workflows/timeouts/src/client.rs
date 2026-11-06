//! Client code that starts a `GreetingsWorkflow` with Workflow timeouts or a Retry Policy.

use std::error::Error;
use std::time::Duration;

use temporalio_client::{Client, WorkflowGetResultOptions, WorkflowStartOptions};
use temporalio_common::RetryPolicy;

use crate::workflows::GreetingsWorkflow;

/// Starts a GreetingsWorkflow with execution, run, and task timeouts, and returns its result.
pub async fn start_with_timeouts(client: &Client) -> Result<String, Box<dyn Error>> {
    // @@@SNIPSTART rust-workflow-timeouts-timeouts
    let wf_handle = client
        .start_workflow(
            GreetingsWorkflow::run,
            (),
            WorkflowStartOptions::new("my-task-queue", "greetings-workflow-10")
                // Set timeouts
                .execution_timeout(Duration::from_secs(3600))
                .run_timeout(Duration::from_secs(600))
                .task_timeout(Duration::from_secs(10))
                .build(),
        )
        .await?;
    // @@@SNIPEND

    Ok(wf_handle
        .get_result(WorkflowGetResultOptions::default())
        .await?)
}

/// Starts a GreetingsWorkflow with a Retry Policy, and returns its result.
pub async fn start_with_retry_policy(client: &Client) -> Result<String, Box<dyn Error>> {
    // @@@SNIPSTART rust-workflow-timeouts-retry-policy
    let wf_handle = client
        .start_workflow(
            GreetingsWorkflow::run,
            (),
            WorkflowStartOptions::new("my-task-queue", "greetings-workflow-11")
                .retry_policy(
                    RetryPolicy::builder()
                        .initial_interval(Duration::from_secs(1))
                        .backoff_coefficient(2.0)
                        .maximum_interval(Duration::from_secs(100))
                        .maximum_attempts(5)
                        .non_retryable_error_types(["NonRetryableError"])
                        .build(),
                )
                .build(),
        )
        .await?;
    // @@@SNIPEND

    Ok(wf_handle
        .get_result(WorkflowGetResultOptions::default())
        .await?)
}

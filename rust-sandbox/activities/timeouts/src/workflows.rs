use std::time::Duration;

use temporalio_common::RetryPolicy;
use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{ActivityOptions, WorkflowContext, WorkflowContextView, WorkflowResult};

use crate::activities::{ActivityLanguages, MyActivities};

/// Schedules Activities with each kind of timeout and a custom Retry Policy.
#[workflow]
pub struct ActivityTimeoutsWorkflow {
    name: String,
}

#[workflow_methods]
impl ActivityTimeoutsWorkflow {
    #[init]
    fn new(_ctx: &WorkflowContextView, name: String) -> Self {
        Self { name }
    }

    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        let name = ctx.state(|s| s.name.clone());

        // @@@SNIPSTART rust-activity-timeouts-start-to-close
        let greeting = ctx
            .execute_activity(
                MyActivities::greet,
                name,
                ActivityOptions::start_to_close_timeout(Duration::from_secs(30)),
            )
            .await?;
        // @@@SNIPEND

        // @@@SNIPSTART rust-activity-timeouts-retry-policy
        let language = ctx
            .execute_activity(
                MyActivities::call_greeting_service,
                ActivityLanguages::English,
                ActivityOptions::with_start_to_close_timeout(Duration::from_secs(30))
                    .retry_policy(
                        RetryPolicy::builder()
                            .initial_interval(Duration::from_secs(10))
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

        let retried_language = language;

        // @@@SNIPSTART rust-activity-timeouts-heartbeat-timeout
        let language = ctx.execute_activity(
            MyActivities::call_greeting_service,
            ActivityLanguages::English,
            ActivityOptions::with_start_to_close_timeout(Duration::from_secs(30))
                .heartbeat_timeout(Duration::from_secs(5))
                .build(),
        );
        // @@@SNIPEND
        let language = language.await?;

        Ok(format!("{greeting} ({retried_language}, {language})"))
    }
}

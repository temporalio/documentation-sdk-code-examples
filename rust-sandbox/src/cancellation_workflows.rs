#![allow(unreachable_pub)]
use std::time::Duration;
use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{ActivityExecutionError, ActivityOptions, WorkflowCancellationToken, WorkflowContext, WorkflowResult};

use crate::cancellation_activities::CancellationActivities;

fn activity_opts() -> ActivityOptions {
    ActivityOptions::with_start_to_close_timeout(Duration::from_secs(300))
        .heartbeat_timeout(Duration::from_secs(5))
        .build()
}

#[workflow]
#[derive(Default)]
pub struct CancellationWorkflow;

#[workflow_methods]
impl CancellationWorkflow {
    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>, _input: ()) -> WorkflowResult<String> {
        let result = ctx
            .execute_activity(
                CancellationActivities::long_running_activity,
                (),
                activity_opts(),
            )
            .await;

        match result {
            Ok(value) => Ok(value),
            Err(ActivityExecutionError::Cancelled(_)) => {
                let reason = ctx.cancellation_token().reason().unwrap_or_default();
                let cleanup_result = ctx
                    .execute_activity(
                        CancellationActivities::cleanup,
                        (),
                        ActivityOptions::with_start_to_close_timeout(Duration::from_secs(10))
                            .cancellation_token(WorkflowCancellationToken::new())
                            .build(),
                    )
                    .await?;

                Ok(format!("Cancelled (reason={reason}), {cleanup_result}"))
            }
            Err(err) => Err(err.into()),
        }
    }
}
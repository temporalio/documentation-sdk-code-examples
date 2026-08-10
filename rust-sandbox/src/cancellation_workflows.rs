#![allow(unreachable_pub)]
use std::time::Duration;
use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{ActivityOptions, WorkflowContext, WorkflowResult};

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
        let mut activity_fut = ctx.start_activity(
            CancellationActivities::long_running_activity,
            (),
            activity_opts(),
        );

        let mut activity_test = ctx.start_activity(
            Test::long_running_activity,
            (),
            activity_opts(),
        );

        ctx.timer(Duration::from_secs(1)).await;

        temporalio_sdk::workflows::select! {
            result = &mut activity_fut => {
                let value = result.map_err(|e| anyhow::anyhow!("{e}"))?;
                Ok(value)
            }
            reason = ctx.cancelled() => {
                activity_fut.cancel();

                let cleanup_result = ctx
                    .start_activity(
                        CancellationActivities::cleanup,
                        (),
                        ActivityOptions::start_to_close_timeout(Duration::from_secs(10)),
                    )
                    .await
                    .map_err(|e| anyhow::anyhow!("{e}"))?;

                Ok(format!("Cancelled (reason={reason}), {cleanup_result}"))
            }
        }
    }
}
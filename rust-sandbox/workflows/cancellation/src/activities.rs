// @@@SNIPSTART rust-cancellation-activities
use std::time::Duration;

use temporalio_macros::activities;
use temporalio_sdk::activities::{ActivityContext, ActivityError};

pub struct CancellationActivities;

#[activities]
impl CancellationActivities {
    #[activity]
    pub async fn long_running_cancellable_activity(
        ctx: ActivityContext,
        _input: (),
    ) -> Result<String, ActivityError> {
        loop {
            // A cancellation request reaches the Activity through its Heartbeats.
            if ctx.is_cancelled() {
                return Err(ActivityError::cancelled());
            }
            ctx.record_heartbeat(()).await?;
            tokio::time::sleep(Duration::from_millis(200)).await;
        }
    }

    #[activity]
    pub async fn cleanup(_ctx: ActivityContext, _input: ()) -> Result<String, ActivityError> {
        Ok("cleanup done".to_string())
    }
}
// @@@SNIPEND

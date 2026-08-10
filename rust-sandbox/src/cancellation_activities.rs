#![allow(unreachable_pub)]
use temporalio_macros::{activities};
use temporalio_sdk::{
    activities::{ActivityContext, ActivityError},
};

pub struct CancellationActivities;

#[activities]
impl CancellationActivities {
    #[activity]
    pub async fn long_running_activity(
        ctx: ActivityContext,
        _input: (),
    ) -> Result<String, ActivityError> {
        loop {
            if ctx.is_cancelled() {
                return Err(ActivityError::cancelled());
            }
            ctx.record_heartbeat(vec![]);
            tokio::time::sleep(std::time::Duration::from_millis(200)).await;
        }
    }

    #[activity]
    pub async fn cleanup(_ctx: ActivityContext, _input: ()) -> Result<String, ActivityError> {
        Ok("cleanup done".to_string())
    }
}
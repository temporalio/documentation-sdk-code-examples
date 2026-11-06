use std::sync::Arc;

use temporalio_macros::activities;
use temporalio_sdk::activities::{ActivityContext, ActivityError};
use tokio::sync::Semaphore;

// @@@SNIPSTART rust-activity-basics-arc
pub struct SleeperActivities {
    pub acts_started: Arc<Semaphore>,
    pub acts_done: Arc<Semaphore>,
}

#[activities]
impl SleeperActivities {
    #[activity]
    pub async fn sleeper(
        self: Arc<Self>,
        ctx: ActivityContext,
        _: String,
    ) -> Result<(), ActivityError> {
        self.acts_started.add_permits(1);
        // just wait to be cancelled
        ctx.cancelled().await;
        self.acts_done.add_permits(1);
        Err(ActivityError::cancelled())
    }
}
// @@@SNIPEND

// @@@SNIPSTART rust-activity-timeouts-next-retry-delay
use std::time::Duration;

use temporalio_macros::activities;
use temporalio_sdk::{
    ApplicationFailure,
    activities::{ActivityContext, ActivityError},
};

pub struct TestGreetActivities;

#[activities]
impl TestGreetActivities {
    #[activity]
    pub async fn greet(ctx: ActivityContext, name: String) -> Result<String, ActivityError> {
        if name == "ziggy" {
            // Wait 5 seconds after the first attempt, 10 after the second, and so on.
            let attempt = ctx.info().attempt;
            return Err(ApplicationFailure::builder("Ziggy is not a valid name")
                .next_retry_delay(Duration::from_secs(5 * u64::from(attempt)))
                .build()
                .into());
        }
        Ok(format!("Hello, {}!", name))
    }
}
// @@@SNIPEND

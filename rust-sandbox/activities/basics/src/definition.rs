// @@@SNIPSTART rust-activity-basics-definition
use temporalio_macros::activities;
use temporalio_sdk::activities::{ActivityContext, ActivityError};

pub struct GreetingActivities;

#[activities]
impl GreetingActivities {
    #[activity]
    pub async fn greet(_ctx: ActivityContext, name: String) -> Result<String, ActivityError> {
        Ok(format!("Hello, {}!", name))
    }

    #[activity]
    pub async fn send_notification(
        _ctx: ActivityContext,
        message: String,
    ) -> Result<(), ActivityError> {
        println!("Sending notification: {}", message);
        Ok(())
    }
}
// @@@SNIPEND

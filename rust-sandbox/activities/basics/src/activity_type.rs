use temporalio_macros::activities;
use temporalio_sdk::activities::{ActivityContext, ActivityError};

pub struct GreetingActivities;

// @@@SNIPSTART rust-activity-basics-activity-type
#[activities]
impl GreetingActivities {
    #[activity(name = "compose_greeting")]
    pub async fn greet(_ctx: ActivityContext, name: String) -> Result<String, ActivityError> {
        Ok(format!("Hello, {}!", name))
    }

    #[activity(name = "send_email")]
    pub async fn send_notification(
        _ctx: ActivityContext,
        message: String,
    ) -> Result<(), ActivityError> {
        println!("Sending notification: {}", message);
        Ok(())
    }
}
// @@@SNIPEND

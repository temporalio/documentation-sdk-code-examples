use temporalio_macros::activities;
use temporalio_sdk::{
    ApplicationFailure,
    activities::{ActivityContext, ActivityError},
};

pub struct HeartbeatActivities;

#[activities]
impl HeartbeatActivities {
    // @@@SNIPSTART rust-activity-timeouts-heartbeat
    #[activity]
    pub async fn greet(ctx: ActivityContext, name: String) -> Result<String, ActivityError> {
        ctx.record_heartbeat("greet activity started".to_string())
            .await?;

        if name == "ziggy" {
            return Err(ApplicationFailure::new("Ziggy is not a valid name").into());
        }

        Ok(format!("Hello, {}!", name))
    }
    // @@@SNIPEND
}

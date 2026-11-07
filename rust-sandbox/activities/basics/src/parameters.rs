// @@@SNIPSTART rust-activity-basics-parameters
use serde::{Deserialize, Serialize};
use temporalio_macros::activities;
use temporalio_sdk::activities::{ActivityContext, ActivityError};

#[derive(Serialize, Deserialize)]
pub struct GreetingInput {
    pub greeting: String,
    pub name: String,
}

pub struct GreetingActivities;

#[activities]
impl GreetingActivities {
    #[activity]
    pub async fn compose_greeting(
        _ctx: ActivityContext,
        input: GreetingInput,
    ) -> Result<String, ActivityError> {
        Ok(format!("{} {}!", input.greeting, input.name))
    }
}
// @@@SNIPEND

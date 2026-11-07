use serde::{Deserialize, Serialize};
use temporalio_macros::activities;
use temporalio_sdk::activities::{ActivityContext, ActivityError};

#[derive(Serialize, Deserialize, Debug, Clone, Copy)]
pub enum ActivityLanguages {
    English,
    Spanish,
}

pub struct MyActivities;

#[activities]
impl MyActivities {
    #[activity]
    pub async fn greet(_ctx: ActivityContext, name: String) -> Result<String, ActivityError> {
        Ok(format!("Hello, {}!", name))
    }

    #[activity]
    pub async fn call_greeting_service(
        _ctx: ActivityContext,
        language: ActivityLanguages,
    ) -> Result<String, ActivityError> {
        let language = match language {
            ActivityLanguages::English => "English",
            ActivityLanguages::Spanish => "Spanish",
        };
        Ok(language.to_string())
    }
}

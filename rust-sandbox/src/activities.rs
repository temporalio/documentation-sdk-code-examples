use temporalio_macros::activities;
use temporalio_sdk::activities::{ActivityContext, ActivityError};
use std::collections::HashMap;
use tokio::time::{sleep, Duration};

#[derive(serde::Serialize, serde::Deserialize, Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum ActivityLanguages {
    Arabic,
    Chinese,
    English,
    French,
    Hindi,
    Spanish,
}

#[derive(serde::Serialize, serde::Deserialize, Debug, Clone, PartialEq, Eq, Hash)]
pub struct ProcessedData {
    pub processed: String,
}

pub struct MyActivities;

#[activities]
impl MyActivities {
    #[activity]
    pub async fn greet(_ctx: ActivityContext, name: String) -> Result<String, ActivityError> {
        Ok(format!("Hello, {}!", name))
    }

    #[activity]
    pub async fn call_greeting_service(_ctx: ActivityContext, to_language: ActivityLanguages) -> Result<String, ActivityError> {
        // Pretend that we are calling a remote service.
        sleep(Duration::from_millis(200)).await;

        let mut greetings = HashMap::new();

        greetings.insert(ActivityLanguages::Arabic, "مرحبا بالعالم".to_string());
        greetings.insert(ActivityLanguages::Chinese, "你好，世界".to_string());
        greetings.insert(ActivityLanguages::English, "Hello, world".to_string());
        greetings.insert(ActivityLanguages::French, "Bonjour, monde".to_string());
        greetings.insert(ActivityLanguages::Hindi, "नमस्ते दुनिया".to_string());
        greetings.insert(ActivityLanguages::Spanish, "Hola mundo".to_string());

        let result = greetings.get(&to_language).cloned();

        Ok(format!("Hello, {:?}!", result))
    }
}
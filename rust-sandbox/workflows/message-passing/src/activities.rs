// @@@SNIPSTART rust-message-passing-activity
use std::{collections::HashMap, time::Duration};

use temporalio_macros::activities;
use temporalio_sdk::activities::{ActivityContext, ActivityError};
use tokio::time::sleep;

use crate::workflows::Language;

pub struct GreetingActivities;

#[activities]
impl GreetingActivities {
    #[activity]
    pub async fn call_greeting_service(
        _ctx: ActivityContext,
        to_language: Language,
    ) -> Result<Option<String>, ActivityError> {
        // Pretend that we are calling a remote service.
        sleep(Duration::from_millis(200)).await;

        let greetings = HashMap::from([
            (Language::Arabic, "مرحبا بالعالم".to_string()),
            (Language::Chinese, "你好，世界".to_string()),
            (Language::English, "Hello, world".to_string()),
            (Language::French, "Bonjour, monde".to_string()),
            (Language::Hindi, "नमस्ते दुनिया".to_string()),
            (Language::Spanish, "Hola mundo".to_string()),
        ]);

        Ok(greetings.get(&to_language).cloned())
    }
}
// @@@SNIPEND

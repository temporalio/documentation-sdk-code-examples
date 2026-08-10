use std::{sync::Arc, time::Duration};

use temporalio_sdk::activities::{ActivityContext, ActivityError};
use temporalio_macros::activities;
use tokio::sync::Semaphore;

#[derive(serde::Serialize, serde::Deserialize, Debug, Clone, PartialEq, Eq, Hash)]
pub struct ProcessedData {
    pub processed: String,
}

pub struct GreetingActivities;

fn validate_input(input: &str) -> bool {
    // For example, we can require that the input is not empty and does not contain any digits.
    !input.is_empty() && !input.chars().any(|c| c.is_digit(10))
}

#[activities]
impl GreetingActivities {
    #[activity(name = "compose_greeting")]
    pub async fn greet(_ctx: ActivityContext, name: String) -> Result<String, ActivityError> {
        Ok(format!("Hello, {}!", name))
    }

    #[activity(name = "send_email")]
    pub async fn send_notification(_ctx: ActivityContext, message: String) -> Result<(), ActivityError> {
        println!("Sending notification: {}", message);
        Ok(())
    }

    #[activity]
    pub async fn process_data(
        _ctx: ActivityContext,
        input: String,
    ) -> Result<ProcessedData, ActivityError> {
        // If an error should be retried
        if !validate_input(&input) {
            return Err(ActivityError::Retryable { 
                source: "Invalid input format".into(), 
                explicit_delay: Some(Duration::from_secs(5)) 
            });
        }

        // If an error should not be retried
        if input.len() > 1000000 {
            return Err(ActivityError::NonRetryable(
                "Input too large".into()
            ));
        }

        let result = ProcessedData {
            processed: input.to_uppercase(),
        };

        Ok(result)
    }
}

// Example of activity that uses Arc
struct SleeperActivities {
    acts_started: Arc<Semaphore>,
    acts_done: Arc<Semaphore>,
}
#[activities]
impl SleeperActivities {
    #[activity]
    async fn sleeper(
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
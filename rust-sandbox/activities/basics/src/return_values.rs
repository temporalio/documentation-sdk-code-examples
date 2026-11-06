use std::time::Duration;

use temporalio_common::error::ApplicationFailure;
use temporalio_macros::activities;
use temporalio_sdk::activities::{ActivityContext, ActivityError};

pub struct MyActivities;

// @@@SNIPSTART rust-activity-basics-return-values
#[derive(serde::Serialize, serde::Deserialize, Debug, Clone, PartialEq, Eq, Hash)]
pub struct ProcessedData {
    pub processed: String,
}

#[activities]
impl MyActivities {
    #[activity]
    pub async fn process_data(
        _ctx: ActivityContext,
        input: String,
    ) -> Result<ProcessedData, ActivityError> {
        // If an error should be retried
        if !validate_input(&input) {
            return Err(ApplicationFailure::builder("Invalid input format")
                .next_retry_delay(Duration::from_secs(5))
                .build()
                .into());
        }

        // If an error should not be retried
        if input.len() > 1000000 {
            return Err(ApplicationFailure::non_retryable("Input too large").into());
        }

        let result = ProcessedData {
            processed: input.to_uppercase(),
        };

        Ok(result)
    }
}
// @@@SNIPEND

/// Accepts non-empty input.
fn validate_input(input: &str) -> bool {
    !input.is_empty()
}

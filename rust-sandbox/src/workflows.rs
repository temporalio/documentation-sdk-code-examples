use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{ActivityOptions, ContinueAsNewOptions, WorkflowContext, WorkflowContextView, WorkflowResult};
use std::time::Duration;
use serde::{Serialize, Deserialize};

use crate::activities::MyActivities;

#[derive(Serialize, Deserialize)]
pub struct GreetingInput {
    pub name: String,
    pub max_history_length: u32,
}

#[workflow]
pub struct GreetingWorkflow {
    pub name: String,
    pub max_history_length: u32,
}

#[workflow_methods]
impl GreetingWorkflow {
    #[init]
    fn new(_ctx: &WorkflowContextView, input: GreetingInput) -> Self {
        Self {
            name: input.name,
            max_history_length: input.max_history_length,
        }
    }

    #[run(name = "greeting-workflow-1")]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        let name = ctx.state(|s| s.name.clone());
        // Execute an activity
        let greeting = ctx.execute_activity(
            MyActivities::greet,
            name,
            ActivityOptions::start_to_close_timeout(Duration::from_secs(30))
        ).await?;

        println!("{}", greeting);

        if greeting.contains("Ziggy") {
            Ok(greeting)
        } else {
            let new_input = GreetingInput {
                name: "New Name".to_string(),
                max_history_length: ctx.state(|s| s.max_history_length),
            };
            // To continue as new, call ctx.continue_as_new and propagate the returned error
            match ctx.continue_as_new(new_input, ContinueAsNewOptions::default())? {}
        }

        // let name = ctx.state(|s| s.name.clone());
        // // Execute an activity
        // let greeting = ctx.execute_activity(
        //     MyActivities::greet,
        //     name,
        //     ActivityOptions::start_to_close_timeout(Duration::from_secs(30))
        // );

        // let language = ctx.execute_activity(
        //     MyActivities::call_greeting_service,
        //     ActivityLanguages::English,
        //     ActivityOptions::with_start_to_close_timeout(Duration::from_secs(30))
        //         .heartbeat_timeout(Duration::from_secs(5))
        //         .retry_policy(
        //             RetryPolicy::builder()
        //                 .initial_interval(Duration::from_secs(10))
        //                 .backoff_coefficient(2.0)
        //                 .maximum_interval(Duration::from_secs(100))
        //                 .maximum_attempts(5)
        //                 .non_retryable_error_types(["NonRetryableError"])
        //                 .build()
        //         ).build()
        // );

        // // Run in parallel
        // let (greeting_res, language_res) = join!(greeting, language);

        // println!("{}", greeting);

        // if greeting.contains("Ziggy") {
        //     Ok(greeting)
        // } else {
        //     let new_input = GreetingInput { name: "New Name".to_string(), max_history_length: 0 };

        //     // To continue as new, call ctx.continue_as_new and propagate the returned error
        //     match ctx.continue_as_new(new_input, ContinueAsNewOptions::default())? {}
        // }
    }

    fn should_continue_as_new(&self, ctx: &WorkflowContext<Self>) -> bool {
        if ctx.continue_as_new_suggested() {
            return true;
        }

        // For testing
        if self.max_history_length > 0 && ctx.history_length() > self.max_history_length {
            return true;
        }

        false
    }
}

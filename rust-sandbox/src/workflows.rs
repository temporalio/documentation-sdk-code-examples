use temporalio_common::prost_dur;
use temporalio_common::protos::coresdk::workflow_commands::ContinueAsNewWorkflowExecution;
use temporalio_common::{protos::temporal::api::common::v1::{RetryPolicy}};
use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::workflows::join;
use temporalio_sdk::{ActivityOptions, WorkflowContext, WorkflowContextView, WorkflowResult, WorkflowTermination};
use std::time::Duration;
use serde::{Serialize, Deserialize};

use crate::{activities::{ActivityLanguages, MyActivities}, workflow_messaging::Language};

#[derive(Serialize, Deserialize)]
pub struct GreetingInput {
    pub name: String,
    pub max_history_length: u32,
}

#[workflow(name = "greeting-workflow-1")]
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

    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        let name = ctx.state(|s| s.name.clone());
        // Execute an activity
        let greeting = ctx.start_activity(
            MyActivities::greet,
            name,
            ActivityOptions::start_to_close_timeout(Duration::from_secs(30))
        ).await?;

        println!("{}", greeting);

        if greeting.contains("Ziggy") {
            Ok(greeting)
        } else {
            let new_input = "New Name".to_string();
            // To continue as new, return an error with WorkflowTermination::ContinueAsNew
            Err(WorkflowTermination::continue_as_new(ContinueAsNewWorkflowExecution {
                workflow_type: "MyWorkflow".to_string(),
                arguments: vec![new_input.into()],
                ..Default::default()
            }))
        }

        // let name = ctx.state(|s| s.name.clone());
        // // Execute an activity
        // let greeting = ctx.start_activity(
        //     MyActivities::greet,
        //     name,
        //     ActivityOptions::start_to_close_timeout(Duration::from_secs(30))
        // );

        // let language = ctx.start_activity(
        //     MyActivities::call_greeting_service,
        //     ActivityLanguages::English,
        //     ActivityOptions::with_start_to_close_timeout(Duration::from_secs(30))
        //         .heartbeat_timeout(Duration::from_secs(5))
        //         .retry_policy(
        //             RetryPolicy {
        //                 initial_interval: Some(prost_dur!(from_secs(10))), 
        //                 backoff_coefficient: 2.0, 
        //                 maximum_interval: Some(prost_dur!(from_secs(100))), 
        //                 maximum_attempts: 5, 
        //                 non_retryable_error_types: vec!["NonRetryableError".to_string()]
        //             }
        //         ).build()
        // );

        // // Run in parallel
        // let (greeting_res, language_res) = join!(greeting, language);

        // println!("{}", greeting);

        // if greeting.contains("Ziggy") {
        //     Ok(greeting)
        // } else {
        //     let new_input = "New Name".to_string();

        //     // To continue as new, return an error with WorkflowTermination::ContinueAsNew
        //     Err(WorkflowTermination::continue_as_new(ContinueAsNewWorkflowExecution {
        //         workflow_type: "MyWorkflow".to_string(),
        //         arguments: vec![new_input.into()],
        //         ..Default::default()
        //     }))
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
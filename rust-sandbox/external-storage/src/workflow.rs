use std::time::Duration;
use temporalio_macros::{activities, workflow, workflow_methods};
use temporalio_sdk::{
    ActivityOptions, WorkflowContext, WorkflowResult,
    activities::{ActivityContext, ActivityError},
};

pub struct LargePayloadActivities;
#[activities]
impl LargePayloadActivities {
    #[activity]
    pub async fn echo(_ctx: ActivityContext, input: String) -> Result<String, ActivityError> {
        Ok(input)
    }
}

#[workflow]
#[derive(Default)]
pub struct LargePayloadWorkflow;
#[workflow_methods]
impl LargePayloadWorkflow {
    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>, input: String) -> WorkflowResult<String> {
        let result = ctx
            .execute_activity(
                LargePayloadActivities::echo,
                input,
                ActivityOptions::start_to_close_timeout(Duration::from_secs(30)),
            )
            .await?;
        Ok(result)
    }
}

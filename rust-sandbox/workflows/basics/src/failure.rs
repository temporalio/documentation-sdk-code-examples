use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{WorkflowContext, WorkflowContextView, WorkflowResult};

#[workflow]
pub struct ValidationWorkflow {
    input: String,
}

#[workflow_methods]
impl ValidationWorkflow {
    #[init]
    fn new(_ctx: &WorkflowContextView, input: String) -> Self {
        Self { input }
    }

    // @@@SNIPSTART rust-workflow-basics-failure
    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        let input_is_empty = ctx.state(|s| s.input.is_empty());
        if input_is_empty {
            return Err(
                temporalio_sdk::ApplicationFailure::new("validation_failed: Input is invalid").into(),
            );
        }

        Ok("Success".to_string())
    }
    // @@@SNIPEND
}

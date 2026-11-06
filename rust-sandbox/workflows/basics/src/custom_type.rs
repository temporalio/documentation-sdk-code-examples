use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{WorkflowContext, WorkflowContextView, WorkflowResult};

// @@@SNIPSTART rust-workflow-basics-custom-type
#[workflow]
pub struct GreetingWorkflow {
    name: String,
}

#[workflow_methods]
impl GreetingWorkflow {
    #[init]
    fn new(_ctx: &WorkflowContextView, name: String) -> Self {
        Self { name }
    }

    #[run(name = "my-custom-workflow")]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        Ok(ctx.state(|s| format!("Hello, {}!", s.name)))
    }
}
// @@@SNIPEND

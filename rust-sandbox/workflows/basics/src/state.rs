use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{WorkflowContext, WorkflowContextView, WorkflowResult};

#[workflow]
pub struct StateWorkflow {
    name: String,
}

#[workflow_methods]
impl StateWorkflow {
    #[init]
    fn new(_ctx: &WorkflowContextView, name: String) -> Self {
        Self { name }
    }

    // @@@SNIPSTART rust-workflow-basics-state
    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        // Read-only access
        let name = ctx.state(|s| s.name.clone());

        // Mutable access
        ctx.state_mut(|s| s.name = format!("{name} (greeted)"));

        Ok(ctx.state(|s| s.name.clone()))
    }
    // @@@SNIPEND
}

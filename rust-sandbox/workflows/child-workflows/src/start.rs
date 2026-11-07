// @@@SNIPSTART rust-child-workflows-start
use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{ChildWorkflowOptions, WorkflowContext, WorkflowContextView, WorkflowResult};

// Child Workflow
#[workflow]
pub struct ComposeGreetingWorkflow {
    pub name: String,
}

#[workflow_methods]
impl ComposeGreetingWorkflow {
    #[init]
    fn new(_ctx: &WorkflowContextView, name: String) -> Self {
        Self { name }
    }

    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        let name = ctx.state(|s| s.name.clone());
        Ok(format!("Hello from child: {}", name))
    }
}

// Parent Workflow
#[workflow]
pub struct GreetingWorkflow {
    pub name: String,
}

#[workflow_methods]
impl GreetingWorkflow {
    #[init]
    fn new(_ctx: &WorkflowContextView, name: String) -> Self {
        Self { name }
    }

    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        let name = ctx.state(|s| s.name.clone());

        let started = ctx
            .start_child_workflow(
                ComposeGreetingWorkflow::run,
                name,
                ChildWorkflowOptions::workflow_id("greeting-child-en".to_string()),
            )
            .await?;

        let result = started.result().await?;

        Ok(format!("ComposeGreetingWorkflow result: {result}"))
    }
}
// @@@SNIPEND

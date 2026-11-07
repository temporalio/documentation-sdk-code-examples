use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{ChildWorkflowOptions, WorkflowContext, WorkflowContextView, WorkflowResult};

#[workflow]
pub struct ComposeEnGreetingWorkflow {
    pub name: String,
}

#[workflow_methods]
impl ComposeEnGreetingWorkflow {
    #[init]
    fn new(_ctx: &WorkflowContextView, name: String) -> Self {
        Self { name }
    }

    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        let name = ctx.state(|s| s.name.clone());
        Ok(format!("Hello: {}", name))
    }
}

#[workflow]
pub struct ComposeEsGreetingWorkflow {
    pub name: String,
}

#[workflow_methods]
impl ComposeEsGreetingWorkflow {
    #[init]
    fn new(_ctx: &WorkflowContextView, name: String) -> Self {
        Self { name }
    }

    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        let name = ctx.state(|s| s.name.clone());
        Ok(format!("Hola: {}", name))
    }
}

#[workflow]
pub struct ParallelGreetingWorkflow {
    pub name: String,
}

#[workflow_methods]
impl ParallelGreetingWorkflow {
    #[init]
    fn new(_ctx: &WorkflowContextView, name: String) -> Self {
        Self { name }
    }

    // @@@SNIPSTART rust-child-workflows-parallel
    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<Vec<Option<String>>> {
        let name = ctx.state(|s| s.name.clone());

        let en_greeting_child = ctx
            .start_child_workflow(
                ComposeEnGreetingWorkflow::run,
                name.clone(),
                ChildWorkflowOptions::workflow_id("greeting-child-en".to_string()),
            )
            .await?;

        let es_greeting_child = ctx
            .start_child_workflow(
                ComposeEsGreetingWorkflow::run,
                name,
                ChildWorkflowOptions::workflow_id("greeting-child-es".to_string()),
            )
            .await?;

        // Both Child Workflows are running. Wait for each result.
        let en_result = en_greeting_child.result().await.ok();
        let es_result = es_greeting_child.result().await.ok();

        Ok(vec![en_result, es_result])
    }
    // @@@SNIPEND
}

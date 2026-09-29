use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{ChildWorkflowOptions, ParentClosePolicy, WorkflowContext, WorkflowContextView, WorkflowResult};

// define child workflow 1
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

// define child workflow 2
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
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<Vec<Option<String>>> {
        let name = ctx.state(|s| s.name.clone());

        let en_greeting_child = ctx.start_child_workflow(
            ComposeEnGreetingWorkflow::run,
            name.clone(),
            ChildWorkflowOptions::workflow_id("greeting-child-en".to_string()),
        ).await?;

        let es_greeting_child = ctx.start_child_workflow(
            ComposeEsGreetingWorkflow::run,
            name.clone(),
            ChildWorkflowOptions::builder()
                .workflow_id("greeting-child-es".to_string())
                .parent_close_policy(ParentClosePolicy::Abandon)
                .build(),
        ).await?;

        let en_result = en_greeting_child.result().await.ok();
        let es_result = es_greeting_child.result().await.ok();

        let combined = vec![en_result, es_result];

        print!("Combined greetings: {:?}", combined);

        Ok(combined)
    }
}

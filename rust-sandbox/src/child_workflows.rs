use temporalio_common::protos::temporal::api::{enums::v1::ParentClosePolicy};
use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{ChildWorkflowOptions, WorkflowContext, WorkflowContextView, WorkflowResult};

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

        let en_greeting_child = ctx.child_workflow(
            ComposeEnGreetingWorkflow::run,
            name.clone(),
            ChildWorkflowOptions {
                workflow_id: format!("greeting-child-en"),
                ..Default::default()
            },
        ).await?;

        let es_greeting_child = ctx.child_workflow(
            ComposeEsGreetingWorkflow::run,
            name.clone(),
            ChildWorkflowOptions {
                workflow_id: format!("greeting-child-es"),
                parent_close_policy: ParentClosePolicy::Abandon,
                ..Default::default()
            },
        ).await?;
        
        let en_result = en_greeting_child.result().await;
        let es_result = es_greeting_child.result().await;

        let combined = vec![en_result, es_result];

        print!("Combined greetings: {:?}", combined);
        
        Ok("".to_string().into())
    }
}
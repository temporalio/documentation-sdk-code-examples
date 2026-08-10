use temporalio_common::protos::{temporal::api::common::v1::Payload};
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

// define workflow 2
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
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<Vec<Option<String>>> {
        let name = ctx.state(|s| s.name.clone());

        let input = vec![
            Payload {
                data: name.as_bytes().to_vec(),
                ..Default::default()
            }
        ];
        let greeting_opts = ChildWorkflowOptions {
            input,
            workflow_id: "compose-greeting-child-workflow-id".to_string(),
            workflow_type: "ComposeGreetingWorkflow".to_string(),
            ..Default::default()
        };

        let en_greeting_child = ctx.child_workflow(greeting_opts.clone()).start().await.into_started().unwrap();
        let es_greeting_child = ctx.child_workflow(ChildWorkflowOptions {
            workflow_id: "compose-spanish-greeting-child-workflow-id".to_string(),
            ..greeting_opts.clone()
        }).start().await.into_started().unwrap();
        
        let en_result = en_greeting_child.result().await.status.map(|s| format!("{:?}", s));
        let es_result = es_greeting_child.result().await.status.map(|s| format!("{:?}", s));

        let combined = vec![en_result, es_result];
        
        Ok(combined)
    }
}
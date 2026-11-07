use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{
    ChildWorkflowOptions, ParentClosePolicy, WorkflowContext, WorkflowContextView, WorkflowResult,
};

use crate::parallel::ComposeEsGreetingWorkflow;

#[workflow]
pub struct AbandonGreetingWorkflow {
    pub name: String,
}

#[workflow_methods]
impl AbandonGreetingWorkflow {
    #[init]
    fn new(_ctx: &WorkflowContextView, name: String) -> Self {
        Self { name }
    }

    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        let name = ctx.state(|s| s.name.clone());

        // @@@SNIPSTART rust-child-workflows-parent-close-policy
        let es_greeting_child = ctx
            .start_child_workflow(
                ComposeEsGreetingWorkflow::run,
                name,
                ChildWorkflowOptions::builder()
                    .workflow_id("greeting-child-es".to_string())
                    .parent_close_policy(ParentClosePolicy::Abandon)
                    .build(),
            )
            .await?;
        // @@@SNIPEND

        Ok(es_greeting_child.result().await?)
    }
}

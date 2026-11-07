use serde::{Deserialize, Serialize};
use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{SyncWorkflowContext, WorkflowContext, WorkflowContextView, WorkflowResult};

#[derive(Clone, Serialize, Deserialize)]
pub struct MyWorkflowInput {
    pub name: String,
}

#[workflow]
pub struct WorkflowRunSeesWorkflowInitWorkflow {
    name_with_title: String,
    title_has_been_checked: bool,
}

// @@@SNIPSTART rust-workflow-basics-constructor
#[workflow_methods]
impl WorkflowRunSeesWorkflowInitWorkflow {
    #[init]
    fn new(_ctx: &WorkflowContextView, workflow_input: MyWorkflowInput) -> Self {
        Self {
            name_with_title: format!("Knight {}", workflow_input.name),
            title_has_been_checked: false,
        }
    }

    #[run]
    pub async fn get_greeting(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        ctx.wait_condition(|state| state.title_has_been_checked).await?;

        let name_with_title = ctx.state(|state| state.name_with_title.clone());
        Ok(format!("Hello, {}", name_with_title))
    }

    #[update]
    pub fn check_title_validity(&mut self, _ctx: &mut SyncWorkflowContext<Self>) -> bool {
        // The Update handler reads state that the #[init] method set,
        // even if the Update arrives before #[run] starts.
        self.title_has_been_checked = true;
        self.name_with_title.starts_with("Knight ")
    }
}
// @@@SNIPEND

use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{SignalWorkflowOptions, WorkflowContext, WorkflowResult};

use crate::workflows::{ApproveInput, GreetingsWorkflow};

/// Sends the approve Signal to another Workflow Execution.
#[workflow]
#[derive(Default)]
pub struct ApproverWorkflow;

#[workflow_methods]
impl ApproverWorkflow {
    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>, workflow_id: String) -> WorkflowResult<()> {
        // @@@SNIPSTART rust-message-passing-signal-from-workflow
        // Pass a Run ID instead of None to target a specific run.
        ctx.external_workflow(workflow_id, None)
            .signal(
                GreetingsWorkflow::approve,
                ApproveInput { name: "Ziggy".to_string() },
                SignalWorkflowOptions::default(),
            )
            .await?;
        // @@@SNIPEND

        Ok(())
    }
}

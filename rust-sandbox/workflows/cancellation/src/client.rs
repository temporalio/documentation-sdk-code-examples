//! Client code that cancels and terminates a `CancellationWorkflow`.

use std::error::Error;

use temporalio_client::{
    Client, WorkflowCancelOptions, WorkflowStartOptions, WorkflowTerminateOptions,
};

use crate::workflows::CancellationWorkflow;

/// Starts a CancellationWorkflow and requests cancellation.
pub async fn cancel_workflow(client: &Client) -> Result<(), Box<dyn Error>> {
    // @@@SNIPSTART rust-cancellation-cancel
    let handle = client
        .start_workflow(
            CancellationWorkflow::run,
            (),
            WorkflowStartOptions::new("my-task-queue", "cancellation-workflow-1").build(),
        )
        .await?;

    handle
        .cancel(WorkflowCancelOptions::builder().reason("No longer needed").build())
        .await?;
    // @@@SNIPEND

    Ok(())
}

/// Starts a CancellationWorkflow and terminates it.
pub async fn terminate_workflow(client: &Client) -> Result<(), Box<dyn Error>> {
    // @@@SNIPSTART rust-cancellation-terminate
    let handle = client
        .start_workflow(
            CancellationWorkflow::run,
            (),
            WorkflowStartOptions::new("my-task-queue", "cancellation-workflow-2").build(),
        )
        .await?;

    handle
        .terminate(WorkflowTerminateOptions::builder().reason("Emergency shutdown").build())
        .await?;
    // @@@SNIPEND

    Ok(())
}

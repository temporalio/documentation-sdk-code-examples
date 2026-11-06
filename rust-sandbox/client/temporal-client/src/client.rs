//! Starts a Workflow Execution and waits for its result.

use std::error::Error;

use temporalio_client::{Client, WorkflowGetResultOptions, WorkflowStartOptions};

use crate::workflows::GreetingsWorkflow;

/// Starts a GreetingsWorkflow on `your-task-queue` and returns its result.
pub async fn start_and_get_result(client: &Client) -> Result<String, Box<dyn Error>> {
    // @@@SNIPSTART rust-temporal-client-start-workflow
    let handle = client
        .start_workflow(
            GreetingsWorkflow::run,
            (),
            WorkflowStartOptions::new("your-task-queue", "your-workflow-id").build(),
        )
        .await?;

    let result = handle
        .get_result(WorkflowGetResultOptions::default())
        .await?;

    println!("Result: {:?}", result);
    // @@@SNIPEND

    Ok(result)
}

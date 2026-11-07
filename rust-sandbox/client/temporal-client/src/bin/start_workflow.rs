// @@@SNIPSTART rust-temporal-client-connect-code
use std::str::FromStr;

use temporal_client::workflows::GreetingsWorkflow;
use temporalio_client::{
    Client, ClientOptions, Connection, ConnectionOptions, Url, WorkflowStartOptions,
};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let connection_options =
        ConnectionOptions::new(Url::from_str("http://localhost:7233")?).build();
    let connection = Connection::connect(connection_options).await?;
    let client = Client::new(connection, ClientOptions::new("default").build())?;

    let wf_handle = client
        .start_workflow(
            GreetingsWorkflow::run,
            (),
            WorkflowStartOptions::new("my-task-queue", "greetings-workflow-10").build(),
        )
        .await?;
    println!("Started Workflow, Run ID: {:?}", wf_handle.run_id());

    Ok(())
}
// @@@SNIPEND

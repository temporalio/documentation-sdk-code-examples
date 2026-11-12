use std::time::{SystemTime, UNIX_EPOCH};

use batch_iterator::{PAGE_SIZE, task_queue, workflows::IteratorBatchWorkflow};
use temporalio_client::{
    Client, ClientOptions, Connection, WorkflowGetResultOptions, WorkflowStartOptions,
    envconfig::LoadClientConfigProfileOptions,
};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Same environment-configuration lookup the Worker uses: TEMPORAL_PROFILE
    // selects the profile, the TOML file supplies address/namespace/credentials,
    // and TEMPORAL_* env vars can override individual fields.
    let (conn_opts, client_opts) =
        ClientOptions::load_from_config(LoadClientConfigProfileOptions::default())?;
    let target = conn_opts.target.clone();
    let namespace = client_opts.namespace.clone();
    let connection = Connection::connect(conn_opts).await?;
    let client = Client::new(connection, client_opts)?;

    let millis = SystemTime::now().duration_since(UNIX_EPOCH)?.as_millis();
    let handle = client
        .start_workflow(
            IteratorBatchWorkflow::run,
            (PAGE_SIZE, 0),
            WorkflowStartOptions::new(task_queue(), format!("batch-iterator-{millis}")).build(),
        )
        .await?;
    println!(
        "Started batch Workflow {} against {target} (namespace: {namespace})...",
        handle.info().workflow_id
    );

    // get_result follows the continue-as-new chain and returns the final run's result.
    let processed = handle
        .get_result(WorkflowGetResultOptions::default())
        .await?;
    println!("Batch complete: processed {processed} records");
    Ok(())
}

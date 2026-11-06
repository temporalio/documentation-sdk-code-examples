use std::error::Error;

use temporalio_client::Client;
use temporalio_sdk::{Runtime, Worker, WorkerOptions};

/// Runs a Worker until the process receives Ctrl-C.
pub async fn run_until_ctrl_c(
    runtime: Runtime,
    client: Client,
    worker_options: WorkerOptions,
) -> Result<(), Box<dyn Error>> {
    // @@@SNIPSTART rust-worker-process-shutdown
    let mut worker = Worker::new(&runtime, client, worker_options)?;
    let shutdown = worker.shutdown_handle();

    tokio::spawn(async move {
        tokio::signal::ctrl_c()
            .await
            .expect("failed to listen for ctrl-c");
        shutdown();
    });

    worker.run().await?;
    // @@@SNIPEND

    Ok(())
}

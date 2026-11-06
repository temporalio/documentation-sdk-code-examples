// @@@SNIPSTART rust-worker-process-run-worker
use std::str::FromStr;

use temporalio_client::{Client, ClientOptions, Connection, ConnectionOptions, Url};
use temporalio_sdk::{Runtime, Worker, WorkerOptions};
use worker_process::{activities::GreetingActivities, workflows::GreetingWorkflow};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let runtime = Runtime::from_current_tokio(Default::default())?;

    let connection_options =
        ConnectionOptions::new(Url::from_str("http://localhost:7233")?).build();
    let connection = Connection::connect(connection_options).await?;
    let client = Client::new(connection, ClientOptions::new("default").build())?;

    let worker_options = WorkerOptions::new("my-task-queue")
        .register_workflow::<GreetingWorkflow>()?
        .register_activities(GreetingActivities)
        .build();

    let mut worker = Worker::new(&runtime, client, worker_options)?;
    worker.run().await?;

    Ok(())
}
// @@@SNIPEND

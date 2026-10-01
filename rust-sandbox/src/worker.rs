use temporalio_client::{
    Client, ClientOptions, Connection,
    envconfig::LoadClientConfigProfileOptions,
};
use temporalio_sdk::{Runtime, Worker, WorkerOptions};

use crate::workflows::GreetingWorkflow;
use crate::activities::MyActivities;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let runtime = Runtime::from_current_tokio(Default::default())?;

    // Set up client connection options, loading from config if available
    let (connection_options, client_options) = ClientOptions::load_from_config(
        LoadClientConfigProfileOptions::default(),
    )?;

    let connection = Connection::connect(connection_options).await?;
    let client = Client::new(connection, client_options)?;

    let worker_options = WorkerOptions::new("my-task-queue")
        .register_activities(MyActivities)
        .register_workflow::<GreetingWorkflow>()?
        .build();

    let mut worker = Worker::new(&runtime, client, worker_options)?;
    worker.run().await?;

    Ok(())
}
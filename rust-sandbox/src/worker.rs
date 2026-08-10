use std::str::FromStr;

use temporalio_client::{Client, ClientOptions, Connection, ConnectionOptions};
use temporalio_common::envconfig::LoadClientConfigProfileOptions;
use temporalio_sdk::{Worker, WorkerOptions};
use temporalio_sdk_core::{CoreRuntime, RuntimeOptions, Url};

use crate::workflows::GreetingWorkflow;
use crate::activities::MyActivities;

#[tokio::main]
pub async fn run() -> Result<(), Box<dyn std::error::Error>> {
    // This shows how you can load a specific profile from a config file.
    let (conn_opts, client_opts) =
        ClientOptions::load_from_config(LoadClientConfigProfileOptions {
            config_file_profile: "prod".to_string().into(),
            ..Default::default()
        })?;

    // Connect to local Temporal server
    let connection_options =
        ConnectionOptions::new(Url::from_str("http://localhost:7233")?).build();

    let runtime = CoreRuntime::new_assume_tokio(RuntimeOptions::builder().build()?)?;

    // Client setup
    let connection = Connection::connect(conn_opts).await?;
    let client = Client::new(connection, client_opts)?;

    let worker_options = WorkerOptions::new("my-task-queue")
        .register_activities(MyActivities)
        .register_workflow::<GreetingWorkflow>()
        .build();

    Worker::new(&runtime, client, worker_options)?.run().await?;
    Ok(())
}
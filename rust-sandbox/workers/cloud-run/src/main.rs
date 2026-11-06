// @@@SNIPSTART rust-cloud-run-worker
use std::str::FromStr;

use my_worker::{activities::MyActivities, workflows::MyWorkflow};
use temporalio_client::{Client, ClientOptions, Connection, ConnectionOptions, TlsOptions, Url};
use temporalio_common::worker::{
    VersioningBehavior, WorkerDeploymentOptions, WorkerDeploymentVersion,
};
use temporalio_sdk::{Runtime, Worker, WorkerOptions};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let address = std::env::var("TEMPORAL_ADDRESS")?;
    let namespace = std::env::var("TEMPORAL_NAMESPACE")?;
    let api_key = std::env::var("TEMPORAL_API_KEY")?;

    let runtime = Runtime::from_current_tokio(Default::default())?;

    let connection_options = ConnectionOptions::new(Url::from_str(&format!("https://{address}"))?)
        .api_key(api_key)
        .tls_options(TlsOptions::default())
        .build();
    let connection = Connection::connect(connection_options).await?;
    let client = Client::new(connection, ClientOptions::new(namespace).build())?;

    let worker_options = WorkerOptions::new(std::env::var("TEMPORAL_TASK_QUEUE")?)
        .deployment_options(
            WorkerDeploymentOptions::new(
                WorkerDeploymentVersion::builder()
                    .deployment_name("my-app")
                    .build_id("build-1")
                    .build(),
            )
            .use_worker_versioning(true)
            .default_versioning_behavior(VersioningBehavior::Pinned)
            .build(),
        )
        .register_workflow::<MyWorkflow>()?
        .register_activities(MyActivities)
        .build();

    let mut worker = Worker::new(&runtime, client, worker_options)?;
    worker.run().await?;

    Ok(())
}
// @@@SNIPEND

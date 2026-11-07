use std::sync::Arc;
use temporal_external_storage_example::{
    local_disk_driver::LocalDiskStorageDriver,
    workflow::{LargePayloadActivities, LargePayloadWorkflow},
};
use temporalio_client::{
    Client, ClientOptions, Connection, WorkflowGetResultOptions, WorkflowStartOptions,
};
use temporalio_common::data_converters::{DataConverter, ExternalStorage, StorageDriver};
use temporalio_sdk::{Runtime, Worker, WorkerOptions};

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    let driver: Arc<dyn StorageDriver> = if let Ok(bucket) = std::env::var("S3_BUCKET") {
        #[cfg(feature = "s3")]
        {
            Arc::new(temporal_external_storage_example::s3_setup::create_driver(bucket).await)
        }
        #[cfg(not(feature = "s3"))]
        {
            let _ = bucket;
            anyhow::bail!("S3_BUCKET requires --features s3");
        }
    } else {
        Arc::new(LocalDiskStorageDriver {
            directory: std::env::current_dir()?.join("payload-store"),
        })
    };
    // @@@SNIPSTART rust-external-storage-setup
    let storage = ExternalStorage::new(driver)?;
    let data_converter = DataConverter::default().with_external_storage(storage);
    let (connection_options, mut client_options) =
        ClientOptions::load_from_config(Default::default())
            .map_err(|error| anyhow::anyhow!("{error}"))?;
    client_options.data_converter = data_converter;
    let connection = Connection::connect(connection_options).await?;
    let client = Client::new(connection, client_options)?;

    let runtime = Runtime::from_current_tokio(Default::default())?;
    let worker_options = WorkerOptions::new("external-storage")
        .register_workflow::<LargePayloadWorkflow>()?
        .register_activities(LargePayloadActivities)
        .build();
    let mut worker = Worker::new(&runtime, client.clone(), worker_options)?;
    // @@@SNIPEND

    let shutdown = worker.shutdown_handle();
    let client_task = async {
        let result = async {
            let handle = client
                .start_workflow(
                    LargePayloadWorkflow::run,
                    "x".repeat(3 * 1024 * 1024),
                    WorkflowStartOptions::new(
                        "external-storage",
                        format!(
                            "external-storage-{}",
                            std::time::SystemTime::now()
                                .duration_since(std::time::UNIX_EPOCH)?
                                .as_nanos()
                        ),
                    )
                    .build(),
                )
                .await?;
            let output = handle
                .get_result(WorkflowGetResultOptions::default())
                .await?;
            anyhow::ensure!(output == "x".repeat(3 * 1024 * 1024), "payload changed");
            println!("Retrieved {} bytes", output.len());
            anyhow::Ok(())
        }
        .await;
        shutdown();
        result
    };
    let (worker_result, client_result) = tokio::join!(worker.run(), client_task);
    worker_result?;
    client_result
}

use batch_iterator::{
    activities::RecordLoader,
    task_queue,
    workflows::{IteratorBatchWorkflow, RecordProcessorWorkflow},
};
use temporalio_client::{
    Client, ClientOptions, Connection, envconfig::LoadClientConfigProfileOptions,
};
use temporalio_sdk::{Runtime, Worker, WorkerOptions};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let runtime = Runtime::from_current_tokio(Default::default())?;

    // load_from_config reads the TEMPORAL_PROFILE env var (default "default"),
    // loads that profile from the TOML config file (default OS config dir, or
    // TEMPORAL_CONFIG_FILE), and applies any TEMPORAL_* overrides. No connection
    // details are hardcoded, so the same Worker runs against a local dev server
    // or Temporal Cloud.
    let (conn_opts, client_opts) =
        ClientOptions::load_from_config(LoadClientConfigProfileOptions::default())?;
    let target = conn_opts.target.clone();
    let namespace = client_opts.namespace.clone();
    let connection = Connection::connect(conn_opts).await?;
    let client = Client::new(connection, client_opts)?;

    let task_queue = task_queue();
    let worker_options = WorkerOptions::new(&task_queue)
        .register_activities(RecordLoader)
        .register_workflow::<IteratorBatchWorkflow>()?
        .register_workflow::<RecordProcessorWorkflow>()?
        .build();
    let mut worker = Worker::new(&runtime, client, worker_options)?;

    println!(
        "Worker connected to {target} (namespace: {namespace}), polling task queue '{task_queue}'"
    );
    worker.run().await?;
    Ok(())
}

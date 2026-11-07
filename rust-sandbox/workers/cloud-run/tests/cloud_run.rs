//! Checks the "Serverless Workers on GCP Cloud Run" examples against a local dev server.
//!
//! The Worker in `src/main.rs` needs Temporal Cloud, so these tests run its Workflow and
//! Activity on a local Worker and check the connection behavior the page describes.
//! Set TEMPORAL_CLI_PATH to use an installed Temporal CLI instead of downloading one.

use std::str::FromStr;

use my_worker::{activities::MyActivities, workflows::MyWorkflow};
use temporalio_client::{
    Connection, ConnectionOptions, Url, WorkflowGetResultOptions, WorkflowStartOptions,
};
use temporalio_sdk::{
    Runtime, Worker, WorkerOptions,
    testing::{EphemeralExe, LocalWorkflowEnvironmentOptions, WorkflowEnvironment},
};

#[tokio::test]
async fn workflow_runs_on_a_worker() {
    let mut options = LocalWorkflowEnvironmentOptions::default();
    if let Ok(path) = std::env::var("TEMPORAL_CLI_PATH") {
        options.server_executable = EphemeralExe::ExistingPath(path);
    }
    let env = WorkflowEnvironment::start_local(options).await.unwrap();
    let client = env.client().clone();

    // An API key can be rotated on the connected Client without restarting the Worker.
    client
        .connection()
        .set_api_key(Some("rotated-key".to_string()));
    client.connection().set_api_key(None);

    let runtime = Runtime::from_current_tokio(Default::default()).unwrap();
    let worker_options = WorkerOptions::new("cloud-run")
        .register_workflow::<MyWorkflow>()
        .unwrap()
        .register_activities(MyActivities)
        .build();
    let mut worker = Worker::new(&runtime, client.clone(), worker_options).unwrap();
    let shutdown = worker.shutdown_handle();

    let (result, run) = tokio::join!(
        async {
            let result = client
                .start_workflow(
                    MyWorkflow::run,
                    "Temporal".to_string(),
                    WorkflowStartOptions::new("cloud-run", "cloud-run").build(),
                )
                .await
                .unwrap()
                .get_result(WorkflowGetResultOptions::default())
                .await
                .unwrap();
            shutdown();
            result
        },
        worker.run()
    );
    run.unwrap();
    assert_eq!(result, "Hello, Temporal!");
    env.shutdown().await.unwrap();
}

#[tokio::test]
async fn api_key_without_tls_options_fails_to_connect() {
    // Anything listening works: the connection fails before TLS would start.
    let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
    let address = listener.local_addr().unwrap();

    let connection_options =
        ConnectionOptions::new(Url::from_str(&format!("https://{address}")).unwrap())
            .api_key("test-key".to_string())
            .build();
    let Err(err) = Connection::connect(connection_options).await else {
        panic!("connected over https without tls_options");
    };
    // The readable message is on an inner error, so collect the whole source chain.
    let mut message = err.to_string();
    let mut source = std::error::Error::source(&err);
    while let Some(inner) = source {
        message.push_str(&format!(": {inner}"));
        source = inner.source();
    }
    assert!(
        message.contains("Connecting to HTTPS without TLS enabled"),
        "{message}"
    );
}

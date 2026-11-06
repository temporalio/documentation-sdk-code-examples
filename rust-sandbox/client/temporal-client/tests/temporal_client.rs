//! Runs the "Temporal Client" examples.
//!
//! The Workflow test starts a local dev server. Set TEMPORAL_CLI_PATH to use an installed
//! Temporal CLI instead of downloading one.

use temporal_client::{
    activities::GreetingActivities,
    client::start_and_get_result,
    workflows::{GreetingsWorkflow, HelloWorldWorkflow},
};
use temporalio_client::{
    ClientOptions, ConnectionOptions,
    envconfig::{DataSource, LoadClientConfigProfileOptions},
};
use temporalio_sdk::{
    Runtime, Worker, WorkerOptions,
    testing::{EphemeralExe, LocalWorkflowEnvironmentOptions, WorkflowEnvironment},
};

/// The `temporal.toml` from the page's "Connect to development Temporal Service" section.
const PAGE_CONFIG: &str = r#"
# Default profile for local development
[profile.default]
address = "localhost:7233"
namespace = "default"

# Optional: Add custom gRPC headers
[profile.default.grpc_meta]
my-custom-header = "development-value"
trace-id = "dev-trace-123"

# Production profile for Temporal Cloud
[profile.prod]
address = "your-namespace.a1b2c.tmprl.cloud:7233"
namespace = "your-namespace"
api_key = "your-api-key-here"

# TLS configuration for production
[profile.prod.tls]
client_cert_path = "/etc/temporal/certs/client.pem"
client_key_path  = "/etc/temporal/certs/client.key"

# Custom headers for production
[profile.prod.grpc_meta]
environment     = "production"
service-version = "v1.2.3"
"#;

/// The Temporal Cloud profiles from the page's "Connect to Temporal Cloud" section.
const CLOUD_CONFIG: &str = r#"
[profile.api]
address = "your-namespace.a1b2c.tmprl.cloud:7233"
namespace = "your-namespace"
api_key = "your-api-key-here"

[profile.mtls]
address = "your-namespace.a1b2c.tmprl.cloud:7233"
namespace = "your-namespace"

[profile.mtls.tls]
client_cert_path = "/path/to/client.pem"
client_key_path = "/path/to/client.key"
"#;

/// Loads `profile` from `config`, ignoring environment variables.
///
/// Certificate paths are read at load time, so the page's placeholder paths are swapped for
/// dummy files.
fn load(config: &str, profile: &str) -> (ConnectionOptions, ClientOptions) {
    let dir = std::env::temp_dir().join(format!("temporal-client-{}", std::process::id()));
    std::fs::create_dir_all(&dir).unwrap();
    let mut config = config.to_string();
    for placeholder in [
        "/etc/temporal/certs/client.pem",
        "/etc/temporal/certs/client.key",
        "/path/to/client.pem",
        "/path/to/client.key",
    ] {
        let file = dir.join(placeholder.replace('/', "_"));
        std::fs::write(&file, "dummy").unwrap();
        config = config.replace(placeholder, file.to_str().unwrap());
    }
    ClientOptions::load_from_config(
        LoadClientConfigProfileOptions::builder()
            .config_source(DataSource::Data(config.as_bytes().to_vec()))
            .config_file_profile(profile.to_owned())
            .disable_env(true)
            .build(),
    )
    .unwrap()
}

#[test]
fn default_profile_targets_local_service_with_headers() {
    let (conn, client) = load(PAGE_CONFIG, "default");
    assert_eq!(conn.target.host_str(), Some("localhost"));
    assert_eq!(conn.target.port(), Some(7233));
    assert_eq!(client.namespace, "default");
    let headers = conn.headers.unwrap();
    assert_eq!(headers["my-custom-header"], "development-value");
    assert_eq!(headers["trace-id"], "dev-trace-123");
    assert!(conn.api_key.is_none());
}

#[test]
fn prod_profile_sets_api_key_tls_and_headers() {
    let (conn, client) = load(PAGE_CONFIG, "prod");
    assert_eq!(
        conn.target.host_str(),
        Some("your-namespace.a1b2c.tmprl.cloud")
    );
    assert_eq!(client.namespace, "your-namespace");
    assert_eq!(conn.api_key.as_deref(), Some("your-api-key-here"));
    assert!(conn.tls_options.is_some());
    assert_eq!(conn.headers.unwrap()["environment"], "production");
}

#[test]
fn cloud_api_key_profile_turns_on_tls() {
    let (conn, client) = load(CLOUD_CONFIG, "api");
    assert_eq!(client.namespace, "your-namespace");
    assert_eq!(conn.api_key.as_deref(), Some("your-api-key-here"));
    assert!(conn.tls_options.is_some());
}

#[test]
fn cloud_mtls_profile_sets_client_certificate() {
    let (conn, _) = load(CLOUD_CONFIG, "mtls");
    assert!(conn.api_key.is_none());
    assert!(conn.tls_options.unwrap().client_tls_options.is_some());
}

#[tokio::test]
async fn start_workflow_and_get_result() {
    let mut options = LocalWorkflowEnvironmentOptions::default();
    if let Ok(path) = std::env::var("TEMPORAL_CLI_PATH") {
        options.server_executable = EphemeralExe::ExistingPath(path);
    }
    let env = WorkflowEnvironment::start_local(options).await.unwrap();
    let client = env.client().clone();

    let runtime = Runtime::from_current_tokio(Default::default()).unwrap();
    let worker_options = WorkerOptions::new("your-task-queue")
        .register_workflow::<GreetingsWorkflow>()
        .unwrap()
        .register_workflow::<HelloWorldWorkflow>()
        .unwrap()
        .register_activities(GreetingActivities)
        .build();
    let mut worker = Worker::new(&runtime, client.clone(), worker_options).unwrap();
    let shutdown = worker.shutdown_handle();

    let (result, run) = tokio::join!(
        async {
            let result = start_and_get_result(&client).await.unwrap();
            shutdown();
            result
        },
        worker.run()
    );
    run.unwrap();
    assert_eq!(result, "Hello, Temporal!");
    env.shutdown().await.unwrap();
}

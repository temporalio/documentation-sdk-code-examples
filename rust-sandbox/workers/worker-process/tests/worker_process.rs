//! Runs the "Run a Worker" examples against a local dev server.
//!
//! Set TEMPORAL_CLI_PATH to use an installed Temporal CLI instead of downloading one.

use temporalio_client::{Client, WorkflowGetResultOptions, WorkflowStartOptions};
use temporalio_common::worker::{
    VersioningBehavior, WorkerDeploymentOptions, WorkerDeploymentVersion,
};
use temporalio_sdk::{
    Runtime, Worker, WorkerOptions,
    testing::{EphemeralExe, LocalServer, LocalWorkflowEnvironmentOptions, WorkflowEnvironment},
};
use worker_process::{
    activities::GreetingActivities,
    registration::worker_options,
    versioned::versioned_worker_options,
    workflows::{GreetingWorkflow, OrderWorkflow},
};

async fn start_env() -> WorkflowEnvironment<LocalServer> {
    let mut options = LocalWorkflowEnvironmentOptions::default();
    if let Ok(path) = std::env::var("TEMPORAL_CLI_PATH") {
        options.server_executable = EphemeralExe::ExistingPath(path);
    }
    WorkflowEnvironment::start_local(options).await.unwrap()
}

/// Runs a Worker built from `options` while `f` runs, then shuts the Worker down.
async fn with_worker<T>(client: &Client, options: WorkerOptions, f: impl AsyncFnOnce() -> T) -> T {
    let runtime = Runtime::from_current_tokio(Default::default()).unwrap();
    let mut worker = Worker::new(&runtime, client.clone(), options).unwrap();
    let shutdown = worker.shutdown_handle();
    let (result, run) = tokio::join!(
        async {
            let result = f().await;
            shutdown();
            result
        },
        worker.run()
    );
    run.unwrap();
    result
}

#[tokio::test]
async fn registered_worker_runs_both_workflows() {
    let env = start_env().await;
    let client = env.client().clone();
    with_worker(&client, worker_options().unwrap(), async || {
        let greeting = client
            .start_workflow(
                GreetingWorkflow::run,
                "Temporal".to_string(),
                WorkflowStartOptions::new("my-task-queue", "greeting").build(),
            )
            .await
            .unwrap()
            .get_result(WorkflowGetResultOptions::default())
            .await
            .unwrap();
        assert_eq!(greeting, "Hello, Temporal!");

        let order = client
            .start_workflow(
                OrderWorkflow::run,
                (),
                WorkflowStartOptions::new("my-task-queue", "order").build(),
            )
            .await
            .unwrap()
            .get_result(WorkflowGetResultOptions::default())
            .await
            .unwrap();
        assert_eq!(order, "Order placed");
    })
    .await;
    env.shutdown().await.unwrap();
}

#[tokio::test]
async fn versioned_worker_options_set_deployment_version() {
    let options = versioned_worker_options().unwrap();
    let deployment = &options.deployment_options;
    assert!(deployment.use_worker_versioning);
    assert_eq!(deployment.version.deployment_name, "my-app");
    assert_eq!(deployment.version.build_id, "1.0");
    assert_eq!(
        deployment.default_versioning_behavior,
        Some(VersioningBehavior::Pinned)
    );

    let env = start_env().await;
    let runtime = Runtime::from_current_tokio(Default::default()).unwrap();
    assert!(Worker::new(&runtime, env.client().clone(), options).is_ok());
    env.shutdown().await.unwrap();
}

#[tokio::test]
async fn versioning_behavior_requires_worker_versioning() {
    let options = WorkerOptions::new("my-task-queue")
        .deployment_options(
            WorkerDeploymentOptions::new(
                WorkerDeploymentVersion::builder()
                    .deployment_name("my-app")
                    .build_id("1.0")
                    .build(),
            )
            .use_worker_versioning(false)
            .default_versioning_behavior(VersioningBehavior::Pinned)
            .build(),
        )
        .register_workflow::<GreetingWorkflow>()
        .unwrap()
        .register_activities(GreetingActivities)
        .build();

    let env = start_env().await;
    let runtime = Runtime::from_current_tokio(Default::default()).unwrap();
    let Err(err) = Worker::new(&runtime, env.client().clone(), options) else {
        panic!("Worker::new accepted a versioning behavior without Worker Versioning");
    };
    assert!(
        err.to_string()
            .contains("default_versioning_behavior must be None"),
        "{err}"
    );
    env.shutdown().await.unwrap();
}

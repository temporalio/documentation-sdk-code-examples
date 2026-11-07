//! Runs the "Nexus feature guide" Workflow against a local dev server.
//!
//! The Rust SDK can't host a Nexus handler, so the test points `my-endpoint` at a Task Queue
//! that nobody polls and checks the Nexus Operation the Workflow schedules.
//! Set TEMPORAL_CLI_PATH to use an installed Temporal CLI instead of downloading one.

use std::time::Duration;

use nexus_feature_guide::workflows::GreetingWorkflow;
use temporalio_client::{
    Client, UntypedWorkflow, WorkflowFetchHistoryOptions, WorkflowStartOptions,
    WorkflowTerminateOptions,
};
use temporalio_common::protos::temporal::api::{
    history::v1::{NexusOperationScheduledEventAttributes, history_event::Attributes},
    nexus::v1::{
        EndpointSpec, EndpointTarget,
        endpoint_target::{Variant, Worker as WorkerTarget},
    },
    operatorservice::v1::CreateNexusEndpointRequest,
};
use temporalio_sdk::{
    Runtime, Worker, WorkerOptions,
    testing::{EphemeralExe, LocalWorkflowEnvironmentOptions, WorkflowEnvironment},
};

const TASK_QUEUE: &str = "nexus-caller";

/// Creates the `my-endpoint` Nexus Endpoint the Workflow calls.
async fn create_endpoint(client: &Client) {
    let request = CreateNexusEndpointRequest {
        spec: Some(EndpointSpec {
            name: "my-endpoint".to_string(),
            target: Some(EndpointTarget {
                variant: Some(Variant::Worker(WorkerTarget {
                    namespace: "default".to_string(),
                    task_queue: "nexus-handler".to_string(),
                })),
            }),
            ..Default::default()
        }),
    };
    client
        .connection()
        .operator_service()
        .create_nexus_endpoint(tonic::IntoRequest::into_request(request))
        .await
        .unwrap();
}

/// Polls history until the Workflow schedules a Nexus Operation.
async fn wait_for_nexus_scheduled(
    client: &Client,
    workflow_id: &str,
) -> NexusOperationScheduledEventAttributes {
    let handle = client.get_workflow_handle::<UntypedWorkflow>(workflow_id);
    for _ in 0..100 {
        let events = handle
            .fetch_history(WorkflowFetchHistoryOptions::default())
            .into_events()
            .await
            .unwrap();
        for event in events {
            if let Some(Attributes::NexusOperationScheduledEventAttributes(attributes)) =
                event.attributes
            {
                return attributes;
            }
        }
        tokio::time::sleep(Duration::from_millis(100)).await;
    }
    panic!("{workflow_id} never scheduled a Nexus Operation");
}

#[tokio::test]
async fn workflow_schedules_nexus_operation() {
    let mut options = LocalWorkflowEnvironmentOptions::default();
    if let Ok(path) = std::env::var("TEMPORAL_CLI_PATH") {
        options.server_executable = EphemeralExe::ExistingPath(path);
    }
    let env = WorkflowEnvironment::start_local(options).await.unwrap();
    let client = env.client().clone();
    create_endpoint(&client).await;

    let runtime = Runtime::from_current_tokio(Default::default()).unwrap();
    let worker_options = WorkerOptions::new(TASK_QUEUE)
        .register_workflow::<GreetingWorkflow>()
        .unwrap()
        .build();
    let mut worker = Worker::new(&runtime, client.clone(), worker_options).unwrap();
    let shutdown = worker.shutdown_handle();

    let (scheduled, run) = tokio::join!(
        async {
            let handle = client
                .start_workflow(
                    GreetingWorkflow::run,
                    "Temporal".to_string(),
                    WorkflowStartOptions::new(TASK_QUEUE, "nexus-caller").build(),
                )
                .await
                .unwrap();
            let scheduled = wait_for_nexus_scheduled(&client, "nexus-caller").await;
            handle
                .terminate(WorkflowTerminateOptions::default())
                .await
                .unwrap();
            shutdown();
            scheduled
        },
        worker.run()
    );
    run.unwrap();

    assert_eq!(scheduled.endpoint, "my-endpoint");
    assert_eq!(scheduled.service, "my-service");
    assert_eq!(scheduled.operation, "my-operation");
    assert_eq!(scheduled.input.unwrap().data, b"\"Temporal\"");
    assert_eq!(scheduled.start_to_close_timeout.unwrap().seconds, 10);
    env.shutdown().await.unwrap();
}

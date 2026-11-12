//! Runs the whole batch against a local dev server.
//!
//! Set TEMPORAL_CLI_PATH to use an installed Temporal CLI instead of downloading one.

use batch_iterator::{
    PAGE_SIZE,
    activities::{PAGE_COUNT, RecordLoader},
    workflows::{IteratorBatchWorkflow, RecordProcessorWorkflow},
};
use temporalio_client::{
    UntypedWorkflow, WorkflowDescribeOptions, WorkflowExecutionStatus, WorkflowGetResultOptions,
    WorkflowStartOptions,
};
use temporalio_sdk::{
    Runtime, Worker, WorkerOptions,
    testing::{EphemeralExe, LocalWorkflowEnvironmentOptions, WorkflowEnvironment},
};

const TASK_QUEUE: &str = "batch-iterator-test";

#[tokio::test]
async fn batch_processes_every_record_across_continue_as_new() {
    let mut options = LocalWorkflowEnvironmentOptions::default();
    if let Ok(path) = std::env::var("TEMPORAL_CLI_PATH") {
        options.server_executable = EphemeralExe::ExistingPath(path);
    }
    let env = WorkflowEnvironment::start_local(options).await.unwrap();
    let client = env.client().clone();

    let runtime = Runtime::from_current_tokio(Default::default()).unwrap();
    let worker_options = WorkerOptions::new(TASK_QUEUE)
        .register_activities(RecordLoader)
        .register_workflow::<IteratorBatchWorkflow>()
        .unwrap()
        .register_workflow::<RecordProcessorWorkflow>()
        .unwrap()
        .build();
    let mut worker = Worker::new(&runtime, client.clone(), worker_options).unwrap();
    let shutdown = worker.shutdown_handle();

    let total = PAGE_SIZE * PAGE_COUNT;
    let workflow_id = "batch-iterator-test";

    let (_, run) = tokio::join!(
        async {
            let handle = client
                .start_workflow(
                    IteratorBatchWorkflow::run,
                    (PAGE_SIZE, 0),
                    WorkflowStartOptions::new(TASK_QUEUE, workflow_id).build(),
                )
                .await
                .unwrap();

            // get_result follows the chain to the last run.
            let result = handle
                .get_result(WorkflowGetResultOptions::default())
                .await
                .unwrap();
            assert_eq!(result, total);

            // The start handle targets the first run, which continued as new.
            let first = handle
                .describe(WorkflowDescribeOptions::default())
                .await
                .unwrap();
            assert_eq!(first.status(), WorkflowExecutionStatus::ContinuedAsNew);

            // Every record got its own completed child Workflow.
            for id in 0..total {
                let child = client
                    .get_workflow_handle::<UntypedWorkflow>(format!("{workflow_id}/{id}"))
                    .describe(WorkflowDescribeOptions::default())
                    .await
                    .unwrap();
                assert_eq!(child.status(), WorkflowExecutionStatus::Completed);
            }
            shutdown();
        },
        worker.run()
    );
    run.unwrap();
    env.shutdown().await.unwrap();
}

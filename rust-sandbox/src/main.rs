use std::{str::FromStr, time::Duration};

use temporalio_client::{Client, ClientOptions, Connection, ConnectionOptions, RetryPolicy, RpcOptions, Url, WorkflowExecuteUpdateOptions, WorkflowGetResultOptions, WorkflowQueryOptions, WorkflowSignalOptions, WorkflowStartOptions, WorkflowStartUpdateOptions, WorkflowTerminateOptions};
use temporalio_sdk::{Runtime, Worker, WorkerOptions};

mod workflows;
mod activities;
mod worker;
mod child_workflows;
mod multiple_workflows;
mod workflow_messaging;
mod cancellation_activities;
mod cancellation_workflows;
mod basic_activities;
mod activities_1;
mod worker_030;
mod docs;

use crate::{activities::MyActivities, workflow_messaging::{ApproveInput, GetLanguagesInput, GreetingsWorkflow, Language, SetLanguageInput}};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let runtime = Runtime::from_current_tokio(Default::default())?;
    let connection_options =
        ConnectionOptions::new(Url::from_str("http://localhost:7233")?).build();

    // Client setup
    let connection = Connection::connect(connection_options).await?;
    let client = Client::new(connection, ClientOptions::new("default").build())?;

    let main_wf_handle = client.start_workflow(
        GreetingsWorkflow::run, 
        (), 
        WorkflowStartOptions::new(
            "my-task-queue",
            "greetings-workflow-10",
        )
        // Set timeouts
        .execution_timeout(Duration::from_secs(3600))
        .run_timeout(Duration::from_secs(600))
        .task_timeout(Duration::from_secs(10))
        .retry_policy(
            RetryPolicy::builder()
                .initial_interval(Duration::from_secs(1))
                .backoff_coefficient(2.0)
                .maximum_interval(Duration::from_secs(100))
                .maximum_attempts(5)
                .non_retryable_error_types(["NonRetryableError"])
                .build()
        ).build()
    ).await?;

    let handle = client
    .start_workflow(
        GreetingsWorkflow::run,
        (),
        WorkflowStartOptions::new(
            "your-task-queue", 
            "your-workflow-id"
        ).build(),
    ).await?;

    let result = handle.get_result(WorkflowGetResultOptions::default()).await;

    println!("Result: {:?}", result);

    let update_handle = main_wf_handle.start_update(
        GreetingsWorkflow::set_language, 
        SetLanguageInput { language: Language::French }, 
        WorkflowStartUpdateOptions::default()
    ).await?;

    let _update_result = update_handle.get_result(RpcOptions::default()).await?;

    // This is an example of starting a workflow with a signal. The signal is delivered atomically with the start, before the workflow's first task runs.
    let _wf_start_with_signal_handle = client.signal_with_start_workflow(
        GreetingsWorkflow::run,
        (),
        GreetingsWorkflow::approve,
        ApproveInput { name: "Ziggy".to_string() },
        WorkflowStartOptions::new("my-task-queue", "greetings-workflow-10").build(),
    ).await?;

    // This is an example of how to terminate a workflow. You can use this to immediately stop a workflow that is no longer needed or to stop a workflow that is stuck or taking too long to complete.
    main_wf_handle.terminate(WorkflowTerminateOptions::builder()
        .reason("Emergency shutdown")
        .build()
    ).await?;

    // This is and example of how to cancel a workflow. You can use this to stop a workflow that is no longer needed or to stop a workflow that is stuck or taking too long to complete.
    // main_wf_handle.cancel(WorkflowCancelOptions::builder().reason("No longer needed").build()).await?;
    
    let worker_options = WorkerOptions::new("my-task-queue")
        .register_activities(MyActivities)
        .register_workflow::<GreetingsWorkflow>()?
        .build();

    let mut worker = Worker::new(&runtime, client, worker_options)?;
    worker.run().await?;

    let supported_languages = main_wf_handle.query(
        GreetingsWorkflow::get_languages, 
        GetLanguagesInput { include_unsupported: true }, 
        WorkflowQueryOptions::default()
    ).await?;

    main_wf_handle.signal(
        GreetingsWorkflow::approve, 
        ApproveInput { name: "Ziggy".to_string() }, 
        WorkflowSignalOptions::default()
    ).await?;

    main_wf_handle.execute_update(
        GreetingsWorkflow::set_language,
        SetLanguageInput { language: Language::French },
        WorkflowExecuteUpdateOptions::default()
    ).await?;

    println!("--------------Supported languages: {:?}", supported_languages); 

    // let handle = client.start_workflow(GreetingWorkflow::run, "Ziggy".to_string(), WorkflowStartOptions::new("my-task-queue", "greeting-workflow-1").build()).await?;

    // let result = handle.get_result(WorkflowGetResultOptions::default()).await?;
    // println!("Started workflow with ID: {}", result);
    Ok(())
}
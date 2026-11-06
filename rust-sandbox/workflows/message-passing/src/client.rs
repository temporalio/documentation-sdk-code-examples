//! Client code that sends Queries, Signals, and Updates to `GreetingsWorkflow`.

use std::error::Error;

use temporalio_client::{
    Client, RpcOptions, WorkflowExecuteUpdateOptions, WorkflowGetResultOptions,
    WorkflowIdConflictPolicy, WorkflowQueryOptions, WorkflowSignalOptions, WorkflowStartOptions,
    WorkflowStartUpdateOptions, WorkflowUpdateWithStartOptions,
};

use crate::workflows::{
    ApproveInput, GetLanguagesInput, GreetingsWorkflow, Language, SetLanguageInput,
};

/// What `send_messages` received back from the Workflow.
#[derive(Debug)]
pub struct MessageResults {
    pub supported_languages: Vec<Language>,
    pub previous_language: Language,
    pub previous_language_from_activity_update: Language,
    pub greeting: String,
}

/// Starts a GreetingsWorkflow, sends it a Query, two Updates, and a Signal, then waits for its result.
pub async fn send_messages(client: &Client) -> Result<MessageResults, Box<dyn Error>> {
    // @@@SNIPSTART rust-message-passing-start
    let wf_handle = client
        .start_workflow(
            GreetingsWorkflow::run,
            (),
            WorkflowStartOptions::new("my-task-queue", "greetings-workflow-10").build(),
        )
        .await?;
    // @@@SNIPEND

    // @@@SNIPSTART rust-message-passing-send-query
    let supported_languages = wf_handle
        .query(
            GreetingsWorkflow::get_languages,
            GetLanguagesInput { include_unsupported: true },
            WorkflowQueryOptions::default(),
        )
        .await?;
    // @@@SNIPEND

    // @@@SNIPSTART rust-message-passing-execute-update
    let previous_language = wf_handle
        .execute_update(
            GreetingsWorkflow::set_language,
            SetLanguageInput { language: Language::Chinese },
            WorkflowExecuteUpdateOptions::default(),
        )
        .await?;
    // @@@SNIPEND

    // @@@SNIPSTART rust-message-passing-start-update
    let update_handle = wf_handle
        .start_update(
            GreetingsWorkflow::set_language_activity,
            SetLanguageInput { language: Language::French },
            WorkflowStartUpdateOptions::default(),
        )
        .await?;

    // Wait for the Update's Activity to finish and fetch the result.
    let previous_language_from_activity_update =
        update_handle.get_result(RpcOptions::default()).await?;
    // @@@SNIPEND

    // @@@SNIPSTART rust-message-passing-send-signal
    wf_handle
        .signal(
            GreetingsWorkflow::approve,
            ApproveInput { name: "Ziggy".to_string() },
            WorkflowSignalOptions::default(),
        )
        .await?;
    // @@@SNIPEND

    let greeting = wf_handle.get_result(WorkflowGetResultOptions::default()).await?;

    Ok(MessageResults {
        supported_languages,
        previous_language,
        previous_language_from_activity_update,
        greeting,
    })
}

/// Starts a GreetingsWorkflow with the approve Signal, then waits for its result.
pub async fn signal_with_start(client: &Client) -> Result<String, Box<dyn Error>> {
    // @@@SNIPSTART rust-message-passing-signal-with-start
    let wf_handle = client
        .signal_with_start_workflow(
            GreetingsWorkflow::run,
            (),
            GreetingsWorkflow::approve,
            ApproveInput { name: "Ziggy".to_string() },
            WorkflowStartOptions::new("my-task-queue", "greetings-workflow-11").build(),
        )
        .await?;
    // @@@SNIPEND

    Ok(wf_handle.get_result(WorkflowGetResultOptions::default()).await?)
}

/// Starts a GreetingsWorkflow, or uses the running one, and sends it the set_language Update.
pub async fn update_with_start(client: &Client) -> Result<Language, Box<dyn Error>> {
    // @@@SNIPSTART rust-message-passing-update-with-start
    let previous_language = client
        .execute_update_with_start_workflow(
            GreetingsWorkflow::run,
            (),
            GreetingsWorkflow::set_language,
            SetLanguageInput { language: Language::Chinese },
            WorkflowUpdateWithStartOptions::new(
                "my-task-queue",
                "greetings-workflow-12",
                WorkflowIdConflictPolicy::UseExisting,
            )
            .build(),
        )
        .await?;
    // @@@SNIPEND

    Ok(previous_language)
}

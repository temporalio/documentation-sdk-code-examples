use human_in_the_loop::{
    WorkflowInput, task_queue, workflow_id, workflows::HumanInTheLoopWorkflow,
};
use temporalio_client::{
    Client, ClientOptions, Connection, WorkflowGetResultOptions, WorkflowStartOptions,
    envconfig::LoadClientConfigProfileOptions, errors::WorkflowStartError,
};

const DEFAULT_REQUEST: &str = "Delete all test data from the production database";

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let user_request = std::env::args()
        .nth(1)
        .unwrap_or_else(|| DEFAULT_REQUEST.to_string());

    // Same environment-configuration lookup the Worker uses: TEMPORAL_PROFILE
    // selects the profile, the TOML file supplies address/namespace/credentials,
    // and TEMPORAL_* env vars can override individual fields.
    let (conn_opts, client_opts) =
        ClientOptions::load_from_config(LoadClientConfigProfileOptions::default())?;
    let target = conn_opts.target.clone();
    let namespace = client_opts.namespace.clone();
    let connection = Connection::connect(conn_opts).await?;
    let client = Client::new(connection, client_opts)?;

    let workflow_id = workflow_id();
    let started = client
        .start_workflow(
            HumanInTheLoopWorkflow::run,
            WorkflowInput {
                user_request: user_request.clone(),
                approval_timeout_seconds: 300,
            },
            WorkflowStartOptions::new(task_queue(), &workflow_id).build(),
        )
        .await;
    let handle = match started {
        Ok(handle) => handle,
        Err(WorkflowStartError::AlreadyStarted { .. }) => {
            println!(
                "Workflow {workflow_id} is already running. Approve or reject it, wait for it \
                 to time out, or terminate it:\n  temporal workflow terminate --workflow-id {workflow_id}"
            );
            std::process::exit(1);
        }
        Err(err) => return Err(err.into()),
    };

    println!("Started Workflow {workflow_id} against {target} (namespace: {namespace})...");
    println!("Request: {user_request}");
    println!("If the action is risky, the Worker prints how to approve or reject it.");

    let result = handle
        .get_result(WorkflowGetResultOptions::default())
        .await?;
    println!("Result: {result}");
    Ok(())
}

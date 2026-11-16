use std::time::SystemTime;

use chrono::{DateTime, SecondsFormat, Utc};
use human_in_the_loop::{ApprovalDecision, workflow_id, workflows::HumanInTheLoopWorkflow};
use temporalio_client::{
    Client, ClientOptions, Connection, WorkflowSignalOptions,
    envconfig::LoadClientConfigProfileOptions,
};

const USAGE: &str = "Usage: cargo run --bin send_approval -- <approve|reject> [notes]";

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let mut args = std::env::args().skip(1);
    let decision = args.next().unwrap_or_default().to_lowercase();
    if decision != "approve" && decision != "reject" {
        println!("{USAGE}");
        std::process::exit(1);
    }
    let notes = args.next();

    // Same environment-configuration lookup the Worker and client use.
    let (conn_opts, client_opts) =
        ClientOptions::load_from_config(LoadClientConfigProfileOptions::default())?;
    let connection = Connection::connect(conn_opts).await?;
    let client = Client::new(connection, client_opts)?;

    // Deliver the reviewer's decision to the waiting Workflow as a Signal. With no
    // request_id, it applies to whichever request is pending.
    let workflow_id = workflow_id();
    client
        .get_workflow_handle::<HumanInTheLoopWorkflow>(&workflow_id)
        .signal(
            HumanInTheLoopWorkflow::approval_decision,
            ApprovalDecision {
                approved: decision == "approve",
                request_id: None,
                reviewer_notes: notes,
                decided_at: Some(
                    DateTime::<Utc>::from(SystemTime::now())
                        .to_rfc3339_opts(SecondsFormat::Secs, true),
                ),
            },
            WorkflowSignalOptions::default(),
        )
        .await?;

    println!("Sent {decision} decision to Workflow {workflow_id}");
    Ok(())
}

// @@@SNIPSTART rust-standalone-activities-list
use futures::StreamExt;
use temporalio_client::{
    ActivityExecutionInfoLike, Client, ClientOptions, Connection,
    envconfig::LoadClientConfigProfileOptions,
};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let (conn_opts, client_opts) =
        ClientOptions::load_from_config(LoadClientConfigProfileOptions::default())?;
    let connection = Connection::connect(conn_opts).await?;
    let client = Client::new(connection, client_opts)?;

    let mut executions =
        client.list_activities("TaskQueue = 'standalone-activities'", Default::default());

    while let Some(execution) = executions.next().await {
        let execution = execution?;
        println!(
            "{} {} {:?}",
            execution.activity_id(),
            execution.activity_type(),
            execution.status()
        );
    }

    Ok(())
}
// @@@SNIPEND

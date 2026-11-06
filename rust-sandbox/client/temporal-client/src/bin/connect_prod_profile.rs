// @@@SNIPSTART rust-temporal-client-connect-profile
use temporalio_client::{
    Client, ClientOptions, Connection, envconfig::LoadClientConfigProfileOptions,
};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let (conn_opts, client_opts) = ClientOptions::load_from_config(
        LoadClientConfigProfileOptions::builder()
            .config_file_profile("prod".to_owned())
            .build(),
    )?;
    let connection = Connection::connect(conn_opts).await?;
    let _client = Client::new(connection, client_opts)?;

    // Use the client to start Workflow Executions, send Signals, and so on.
    println!("Connected with the prod profile!");

    Ok(())
}
// @@@SNIPEND

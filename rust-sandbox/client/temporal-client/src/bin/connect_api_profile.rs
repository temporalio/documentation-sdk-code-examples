// @@@SNIPSTART rust-temporal-client-cloud-profile
use temporalio_client::{
    Client, ClientOptions, Connection, envconfig::LoadClientConfigProfileOptions,
};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let (conn_opts, client_opts) = ClientOptions::load_from_config(
        LoadClientConfigProfileOptions::builder()
            .config_file_profile("api".to_owned())
            .build(),
    )?;

    // Client setup
    let connection = Connection::connect(conn_opts).await?;
    let _client = Client::new(connection, client_opts)?;

    println!("Connected to Temporal Cloud!");
    Ok(())
}
// @@@SNIPEND

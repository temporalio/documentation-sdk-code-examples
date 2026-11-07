// @@@SNIPSTART rust-temporal-client-cloud-code
use std::str::FromStr;

use temporalio_client::{Client, ClientOptions, Connection, ConnectionOptions, TlsOptions, Url};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let connection_options = ConnectionOptions::new(Url::from_str(
        "https://your-namespace.a1b2c.tmprl.cloud:7233",
    )?)
    .api_key("your-api-key")
    // An API key doesn't turn on TLS by itself.
    .tls_options(TlsOptions::default())
    .build();

    let connection = Connection::connect(connection_options).await?;
    let _client = Client::new(connection, ClientOptions::new("your-namespace").build())?;

    println!("Connected to Temporal Cloud!");
    Ok(())
}
// @@@SNIPEND

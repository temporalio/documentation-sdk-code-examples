use anyhow::anyhow;
use aws_sdk_s3::{Client, primitives::ByteStream};
use futures::future::BoxFuture;
use prost::Message;
use sha2::{Digest, Sha256};
use std::collections::HashMap;
use temporalio_common::{
    data_converters::{
        StorageDriver, StorageDriverClaim, StorageDriverError, StorageDriverRetrieveContext,
        StorageDriverStoreContext,
    },
    protos::temporal::api::common::v1::Payload,
};

// @@@SNIPSTART rust-external-storage-s3-driver
// Example driver, not a built-in Temporal driver.
#[derive(Clone)]
pub struct S3StorageDriver {
    pub client: Client,
    pub bucket: String,
}

impl StorageDriver for S3StorageDriver {
    fn name(&self) -> &str {
        "s3-payloads"
    }
    fn driver_type(&self) -> &str {
        "example.s3"
    }

    fn store(
        &self,
        context: &StorageDriverStoreContext,
        payloads: Vec<Payload>,
    ) -> BoxFuture<'static, Result<Vec<StorageDriverClaim>, StorageDriverError>> {
        let driver = self.clone();
        let limiter = context.limiter.clone();
        Box::pin(async move {
            let mut claims = Vec::with_capacity(payloads.len());
            for payload in payloads {
                let _permit = limiter.acquire(&payload).await?;
                let bytes = payload.encode_to_vec();
                let checksum = format!("{:x}", Sha256::digest(&bytes));
                let key = format!("temporal-payloads/{checksum}");
                driver
                    .client
                    .put_object()
                    .bucket(&driver.bucket)
                    .key(&key)
                    .body(ByteStream::from(bytes))
                    .send()
                    .await
                    .map_err(|e| anyhow::Error::new(e).context("S3 upload failed"))?;
                claims.push(StorageDriverClaim::new(HashMap::from([
                    ("key".to_owned(), key),
                    ("sha256".to_owned(), checksum),
                ])));
            }
            Ok(claims)
        })
    }

    fn retrieve(
        &self,
        context: &StorageDriverRetrieveContext,
        claims: Vec<StorageDriverClaim>,
    ) -> BoxFuture<'static, Result<Vec<Payload>, StorageDriverError>> {
        let driver = self.clone();
        let limiter = context.limiter.clone();
        Box::pin(async move {
            let mut payloads = Vec::with_capacity(claims.len());
            for claim in claims {
                let _permit = limiter.acquire(&claim).await?;
                let key = claim
                    .claim_data
                    .get("key")
                    .ok_or_else(|| anyhow!("claim is missing its key"))?;
                let checksum = claim
                    .claim_data
                    .get("sha256")
                    .ok_or_else(|| anyhow!("claim is missing its checksum"))?;
                if !(checksum.len() == 64
                    && checksum.bytes().all(|b| b.is_ascii_hexdigit())
                    && *key == format!("temporal-payloads/{checksum}"))
                {
                    return Err(anyhow!("invalid S3 claim").into());
                }
                let response = driver
                    .client
                    .get_object()
                    .bucket(&driver.bucket)
                    .key(key)
                    .send()
                    .await
                    .map_err(|e| anyhow::Error::new(e).context("S3 download failed"))?;
                let bytes = response
                    .body
                    .collect()
                    .await
                    .map_err(|e| anyhow::Error::new(e).context("S3 body read failed"))?
                    .into_bytes();
                if !(format!("{:x}", Sha256::digest(&bytes)) == *checksum) {
                    return Err(anyhow!("payload checksum mismatch").into());
                }
                payloads.push(Payload::decode(bytes.as_ref())?);
            }
            Ok(payloads)
        })
    }
}
// @@@SNIPEND

pub async fn create_driver(bucket: String) -> S3StorageDriver {
    // @@@SNIPSTART rust-external-storage-s3-create
    let config = aws_config::load_defaults(aws_config::BehaviorVersion::latest()).await;
    let driver = S3StorageDriver {
        client: aws_sdk_s3::Client::new(&config),
        bucket,
    };
    // @@@SNIPEND
    driver
}

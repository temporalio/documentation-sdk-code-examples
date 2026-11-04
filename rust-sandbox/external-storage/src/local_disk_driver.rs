use anyhow::anyhow;
use futures::future::BoxFuture;
use prost::Message;
use sha2::{Digest, Sha256};
use std::{collections::HashMap, path::PathBuf};
use temporalio_common::{
    data_converters::{
        StorageDriver, StorageDriverClaim, StorageDriverError, StorageDriverRetrieveContext,
        StorageDriverStoreContext,
    },
    protos::temporal::api::common::v1::Payload,
};

// @@@SNIPSTART rust-external-storage-local-driver
// For local development only. All processes must share this directory.
#[derive(Clone)]
pub struct LocalDiskStorageDriver {
    pub directory: PathBuf,
}

impl StorageDriver for LocalDiskStorageDriver {
    fn name(&self) -> &str {
        "local-disk"
    }
    fn driver_type(&self) -> &str {
        "example.local-disk"
    }

    fn store(
        &self,
        context: &StorageDriverStoreContext,
        payloads: Vec<Payload>,
    ) -> BoxFuture<'static, Result<Vec<StorageDriverClaim>, StorageDriverError>> {
        let directory = self.directory.clone();
        let limiter = context.limiter.clone();
        Box::pin(async move {
            tokio::fs::create_dir_all(&directory).await?;
            let mut claims = Vec::with_capacity(payloads.len());
            for payload in payloads {
                let _permit = limiter.acquire(&payload).await?;
                let bytes = payload.encode_to_vec();
                let key = format!("{:x}", Sha256::digest(&bytes));
                // Identical payloads use the same key, including during replay.
                tokio::fs::write(directory.join(&key), bytes).await?;
                claims.push(StorageDriverClaim::new(HashMap::from([(
                    "key".to_owned(),
                    key,
                )])));
            }
            Ok(claims)
        })
    }

    fn retrieve(
        &self,
        context: &StorageDriverRetrieveContext,
        claims: Vec<StorageDriverClaim>,
    ) -> BoxFuture<'static, Result<Vec<Payload>, StorageDriverError>> {
        let directory = self.directory.clone();
        let limiter = context.limiter.clone();
        Box::pin(async move {
            let mut payloads = Vec::with_capacity(claims.len());
            for claim in claims {
                let _permit = limiter.acquire(&claim).await?;
                let key = claim
                    .claim_data
                    .get("key")
                    .ok_or_else(|| anyhow!("claim is missing its key"))?;
                // Accept only a hash, never a path supplied by a claim.
                if !(key.len() == 64 && key.bytes().all(|b| b.is_ascii_hexdigit())) {
                    return Err(anyhow!("invalid payload key").into());
                }
                let bytes = tokio::fs::read(directory.join(key)).await?;
                if !(format!("{:x}", Sha256::digest(&bytes)) == *key) {
                    return Err(anyhow!("payload checksum mismatch").into());
                }
                payloads.push(Payload::decode(bytes.as_slice())?);
            }
            Ok(payloads)
        })
    }
}
// @@@SNIPEND

use super::local_disk_driver::LocalDiskStorageDriver;
use futures::future::BoxFuture;
use std::{collections::HashMap, sync::Arc};
use temporalio_common::{
    data_converters::{
        StorageDriver, StorageDriverError, StorageDriverLimiter, StorageDriverPermit,
        StorageDriverRetrieveContext, StorageDriverStoreContext,
    },
    protos::temporal::api::common::v1::Payload,
};
struct Unlimited;
impl<T: Send + Sync> StorageDriverLimiter<T> for Unlimited {
    fn acquire<'a>(
        &'a self,
        _item: &'a T,
    ) -> BoxFuture<'a, Result<StorageDriverPermit, StorageDriverError>> {
        Box::pin(async { Ok(StorageDriverPermit::new(Box::new(()))) })
    }
}
fn store_context() -> StorageDriverStoreContext {
    StorageDriverStoreContext::builder()
        .limiter(Arc::new(Unlimited))
        .build()
}
fn retrieve_context() -> StorageDriverRetrieveContext {
    StorageDriverRetrieveContext::builder()
        .limiter(Arc::new(Unlimited))
        .build()
}
#[tokio::test]
async fn local_disk_round_trip() {
    let dir = tempfile::tempdir().unwrap();
    let driver = LocalDiskStorageDriver {
        directory: dir.path().to_owned(),
    };
    let inputs = vec![
        Payload {
            metadata: HashMap::from([("encoding".into(), b"json/plain".to_vec())]),
            data: b"first".to_vec(),
            ..Default::default()
        },
        Payload {
            data: vec![42; 3 * 1024 * 1024],
            ..Default::default()
        },
    ];
    let claims = driver
        .store(&store_context(), inputs.clone())
        .await
        .unwrap();
    let output = driver.retrieve(&retrieve_context(), claims).await.unwrap();
    assert_eq!(output, inputs);
}

/// Stores and retrieves real S3 objects. Use a dedicated test bucket.
#[cfg(feature = "s3")]
#[tokio::test]
#[ignore = "requires AWS credentials and a disposable RUST_EXTERNAL_STORAGE_TEST_BUCKET"]
async fn s3_live_round_trip() {
    use crate::s3_setup::create_driver;

    let bucket = std::env::var("RUST_EXTERNAL_STORAGE_TEST_BUCKET")
        .expect("set RUST_EXTERNAL_STORAGE_TEST_BUCKET to a disposable bucket");
    let config = aws_config::load_defaults(aws_config::BehaviorVersion::latest()).await;
    let client = aws_sdk_s3::Client::new(&config);
    let driver = create_driver(bucket.clone()).await;
    let nonce = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap()
        .as_nanos()
        .to_string();
    let inputs = vec![
        Payload {
            metadata: HashMap::from([
                ("encoding".into(), b"json/plain".to_vec()),
                ("test-run".into(), nonce.as_bytes().to_vec()),
            ]),
            data: vec![42; 3 * 1024 * 1024],
            ..Default::default()
        },
        Payload {
            data: nonce.as_bytes().to_vec(),
            ..Default::default()
        },
    ];
    let claims = driver
        .store(&store_context(), inputs.clone())
        .await
        .unwrap();
    // A fresh instance must retrieve claims without any in-memory state from store().
    let reader = create_driver(bucket.clone()).await;
    assert_eq!(
        reader
            .retrieve(&retrieve_context(), claims.clone())
            .await
            .unwrap(),
        inputs
    );

    // Remove only the objects this test stored.
    for claim in claims {
        client
            .delete_object()
            .bucket(&bucket)
            .key(&claim.claim_data["key"])
            .send()
            .await
            .unwrap();
    }
}

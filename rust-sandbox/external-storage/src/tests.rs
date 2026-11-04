use super::local_disk_driver::LocalDiskStorageDriver;
use futures::future::BoxFuture;
use std::{collections::HashMap, sync::Arc};
use temporalio_common::{
    data_converters::{
        StorageDriver, StorageDriverClaim, StorageDriverError, StorageDriverLimiter,
        StorageDriverPermit, StorageDriverRetrieveContext, StorageDriverStoreContext,
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
async fn preserves_metadata_order_and_deduplicates_replayed_writes() {
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
    let repeated = driver
        .store(&store_context(), inputs.clone())
        .await
        .unwrap();
    assert_eq!(claims, repeated);
    let output = driver.retrieve(&retrieve_context(), claims).await.unwrap();
    assert_eq!(output, inputs);
    assert_eq!(std::fs::read_dir(dir.path()).unwrap().count(), 2);
}
#[tokio::test]
async fn rejects_corrupt_missing_and_invalid_claims() {
    let dir = tempfile::tempdir().unwrap();
    let driver = LocalDiskStorageDriver {
        directory: dir.path().to_owned(),
    };
    let claims = driver
        .store(&store_context(), vec![Payload::default()])
        .await
        .unwrap();
    let path = dir.path().join(&claims[0].claim_data["key"]);
    tokio::fs::write(&path, b"corrupt").await.unwrap();
    assert!(
        driver
            .retrieve(&retrieve_context(), claims.clone())
            .await
            .is_err()
    );
    tokio::fs::remove_file(&path).await.unwrap();
    assert!(driver.retrieve(&retrieve_context(), claims).await.is_err());
    for claim in [
        StorageDriverClaim::new(HashMap::new()),
        StorageDriverClaim::new(HashMap::from([("key".into(), "../outside".into())])),
    ] {
        assert!(
            driver
                .retrieve(&retrieve_context(), vec![claim])
                .await
                .is_err()
        );
    }
}

/// Writes and corrupts objects. Run only against a dedicated, disposable bucket.
#[cfg(feature = "s3")]
#[tokio::test]
#[ignore = "requires AWS credentials and a disposable RUST_EXTERNAL_STORAGE_TEST_BUCKET"]
async fn s3_live_round_trip_and_failure_paths() {
    use crate::s3_driver::create_driver;
    use aws_sdk_s3::primitives::ByteStream;

    let bucket = std::env::var("RUST_EXTERNAL_STORAGE_TEST_BUCKET")
        .expect("set RUST_EXTERNAL_STORAGE_TEST_BUCKET to a disposable bucket");
    let driver = create_driver(bucket).await;
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
    assert_eq!(claims.len(), inputs.len());
    let repeated = driver
        .store(&store_context(), inputs.clone())
        .await
        .unwrap();
    assert_eq!(claims, repeated);
    // A fresh instance must retrieve claims without any in-memory state from store().
    let reader = create_driver(driver.bucket.clone()).await;
    assert_eq!(
        reader
            .retrieve(&retrieve_context(), claims.clone())
            .await
            .unwrap(),
        inputs
    );

    let key = &claims[0].claim_data["key"];
    driver
        .client
        .put_object()
        .bucket(&driver.bucket)
        .key(key)
        .body(ByteStream::from_static(b"corrupt"))
        .send()
        .await
        .unwrap();
    let corrupt = reader
        .retrieve(&retrieve_context(), vec![claims[0].clone()])
        .await
        .unwrap_err()
        .into_source();
    assert!(
        corrupt.to_string().contains("checksum mismatch"),
        "{corrupt:#}"
    );

    driver
        .client
        .delete_object()
        .bucket(&driver.bucket)
        .key(key)
        .send()
        .await
        .unwrap();
    assert!(
        reader
            .retrieve(&retrieve_context(), vec![claims[0].clone()])
            .await
            .is_err()
    );
    for claim in [
        StorageDriverClaim::new(HashMap::new()),
        StorageDriverClaim::new(HashMap::from([
            ("key".into(), "../outside".into()),
            ("sha256".into(), "0".repeat(64)),
        ])),
    ] {
        assert!(
            reader
                .retrieve(&retrieve_context(), vec![claim])
                .await
                .is_err()
        );
    }
    // Remove only the objects this test stored.
    for claim in claims {
        driver
            .client
            .delete_object()
            .bucket(&driver.bucket)
            .key(&claim.claim_data["key"])
            .send()
            .await
            .unwrap();
    }
}

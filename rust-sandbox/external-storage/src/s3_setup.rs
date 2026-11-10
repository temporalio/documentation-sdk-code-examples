use temporalio_aws_s3driver::S3StorageDriver;

pub async fn create_driver(bucket: String) -> S3StorageDriver {
    // @@@SNIPSTART rust-external-storage-s3-create
    use std::sync::Arc;
    use temporalio_aws_s3driver::{
        AwsSdkS3StorageDriverClient, S3StorageDriver, S3StorageDriverOptions,
    };

    let config = aws_config::load_defaults(aws_config::BehaviorVersion::latest()).await;
    let driver = S3StorageDriver::new(
        S3StorageDriverOptions::builder()
            .client(Arc::new(AwsSdkS3StorageDriverClient::new(
                aws_sdk_s3::Client::new(&config),
            )))
            .bucket(bucket)
            .build(),
    );
    // @@@SNIPEND
    driver
}

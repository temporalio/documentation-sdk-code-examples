package io.temporal.docs.externalstorage;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.payload.storage.ExternalStorage;
import io.temporal.payload.storage.s3driver.S3StorageDriver;
import io.temporal.payload.storage.s3driver.awssdkv2.S3AsyncClientAdapter;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;

class S3StorageDriverExamples {
  static S3StorageDriver createDriver() {
    // @@@SNIPSTART java-s3-driver-create
    S3AsyncClient s3Client = S3AsyncClient.builder().region(Region.US_EAST_2).build();

    S3StorageDriver driver =
        S3StorageDriver.newBuilder()
            .setClient(new S3AsyncClientAdapter(s3Client))
            .setBucket("my-temporal-payloads")
            .build();
    // @@@SNIPEND
    return driver;
  }

  static void configureClientAndWorker(S3StorageDriver driver) {
    // @@@SNIPSTART java-s3-external-storage-setup
    ExternalStorage externalStorage = ExternalStorage.newBuilder().setDriver(driver).build();

    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client =
        WorkflowClient.newInstance(
            service,
            WorkflowClientOptions.newBuilder().setExternalStorage(externalStorage).build());
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker("my-task-queue");
    // @@@SNIPEND
    factory.shutdown();
    service.shutdown();
  }

  static S3StorageDriver createMultiRegionDriver() {
    // @@@SNIPSTART java-s3-mrap-driver-create
    S3AsyncClient s3Client =
        S3AsyncClient.builder()
            .region(Region.US_EAST_2)
            .serviceConfiguration(S3Configuration.builder().useArnRegionEnabled(true).build())
            .build();

    return S3StorageDriver.newBuilder()
        .setClient(new S3AsyncClientAdapter(s3Client))
        .setBucket("arn:aws:s3::123456789012:accesspoint/example.mrap")
        .build();
    // @@@SNIPEND
  }
}

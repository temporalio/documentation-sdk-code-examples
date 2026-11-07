package io.temporal.docs.bestpractices.externalstorage;

import io.temporal.payload.storage.s3driver.S3StorageDriverClient;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Stands in for Amazon S3 so the S3 driver can run without AWS. */
class InMemoryS3Client implements S3StorageDriverClient {
    final Map<String, byte[]> objects = new ConcurrentHashMap<>();
    final AtomicInteger puts = new AtomicInteger();

    @Override
    public CompletableFuture<Void> putObject(String bucket, String key, byte[] data) {
        puts.incrementAndGet();
        objects.put(bucket + "/" + key, data.clone());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Boolean> objectExists(String bucket, String key) {
        return CompletableFuture.completedFuture(objects.containsKey(bucket + "/" + key));
    }

    @Override
    public CompletableFuture<byte[]> getObject(String bucket, String key) {
        byte[] data = objects.get(bucket + "/" + key);
        if (data == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("No object " + key));
        }
        return CompletableFuture.completedFuture(data.clone());
    }
}

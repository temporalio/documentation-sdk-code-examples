package io.temporal.docs.externalstorage;

import io.temporal.payload.storage.ExternalStorage;
import io.temporal.payload.storage.StorageDriver;
import java.util.Arrays;

class ExternalStorageConfigurationExamples {
  static ExternalStorage configureThreshold(StorageDriver driver) {
    // @@@SNIPSTART java-external-storage-threshold
    return ExternalStorage.newBuilder().setDriver(driver).setPayloadSizeThreshold(0).build();
    // @@@SNIPEND
  }

  static ExternalStorage configureMultipleDrivers(
      StorageDriver preferredDriver, StorageDriver legacyDriver) {
    // @@@SNIPSTART java-external-storage-multiple-drivers
    return ExternalStorage.newBuilder()
        .setDrivers(Arrays.asList(preferredDriver, legacyDriver))
        .setDriverSelector((context, payload) -> preferredDriver)
        .build();
    // @@@SNIPEND
  }
}

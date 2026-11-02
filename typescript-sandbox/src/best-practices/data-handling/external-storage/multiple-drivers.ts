import { ExternalStorage } from '@temporalio/common';
import { S3StorageDriver } from '@temporalio/external-storage-s3';
import { AwsSdkS3StorageDriverClient } from '@temporalio/external-storage-s3-aws-sdk';
import { FileSystemStorageDriver } from './file-system-storage-driver';
import { STORAGE_ROOT } from './data-converter';
import { s3Client } from './s3-driver';

// The previous backend, kept registered so existing claims can still be retrieved
class LegacyStorageDriver extends FileSystemStorageDriver {
  constructor() {
    super({ rootDir: STORAGE_ROOT, name: 'legacy.filesystem' });
  }
}

export function createExternalStorage(): ExternalStorage {
  // @@@SNIPSTART typescript-best-practices-external-storage-multiple-drivers
  const preferredDriver = new S3StorageDriver({
    client: new AwsSdkS3StorageDriverClient(s3Client),
    bucket: 'my-bucket',
  });
  const legacyDriver = new LegacyStorageDriver();

  const externalStorage = new ExternalStorage({
    drivers: [preferredDriver, legacyDriver],
    driverSelector: () => preferredDriver,
  });
  // @@@SNIPEND
  return externalStorage;
}

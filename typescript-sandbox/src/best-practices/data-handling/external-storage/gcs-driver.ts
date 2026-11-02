// @@@SNIPSTART typescript-best-practices-external-storage-gcs-driver-create
import { Storage } from '@google-cloud/storage';
import { GcsStorageDriver } from '@temporalio/external-storage-gcs';
import { GoogleCloudGcsStorageDriverClient } from '@temporalio/external-storage-gcs-google-sdk';

const storage = new Storage();

const driver = new GcsStorageDriver({
  client: new GoogleCloudGcsStorageDriverClient(storage),
  bucket: 'my-temporal-payloads',
});
// @@@SNIPEND

export { driver };

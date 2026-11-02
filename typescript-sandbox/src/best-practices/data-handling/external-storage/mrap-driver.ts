// @@@SNIPSTART typescript-best-practices-external-storage-sigv4a-import
import '@aws-sdk/signature-v4a';
// @@@SNIPEND
import { S3StorageDriver } from '@temporalio/external-storage-s3';
import { AwsSdkS3StorageDriverClient } from '@temporalio/external-storage-s3-aws-sdk';
import { s3Client } from './s3-driver';

// @@@SNIPSTART typescript-best-practices-external-storage-mrap-driver
const driver = new S3StorageDriver({
  client: new AwsSdkS3StorageDriverClient(s3Client),
  bucket: 'arn:aws:s3::123456789012:accesspoint/mfzwi23gnjvgw.mrap',
});
// @@@SNIPEND

export { driver };

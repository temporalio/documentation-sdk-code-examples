import os from 'os';
import path from 'path';
import { DataConverter, ExternalStorage } from '@temporalio/common';
import { FileSystemStorageDriver } from './file-system-storage-driver';

export const STORAGE_ROOT = path.join(os.tmpdir(), 'temporal-payloads');
export const PAYLOAD_SIZE_THRESHOLD = 256 * 1024;

// @@@SNIPSTART typescript-best-practices-external-storage-custom-driver-data-converter
export function createDataConverter(rootDir: string = STORAGE_ROOT): DataConverter {
  return {
    externalStorage: new ExternalStorage({
      drivers: [new FileSystemStorageDriver({ rootDir })],
      payloadSizeThreshold: PAYLOAD_SIZE_THRESHOLD,

      // With one driver registered, every offloaded payload goes to it. Register more
      // than one and a `driverSelector` becomes required, letting you route per
      // payload: a cheap archive tier for a known-bulky Workflow type, a driver per
      // tenant, or a per-region bucket. Returning `null` from the selector keeps that
      // payload inline, which is how you exempt specific payloads from offloading.
      //
      // driverSelector: (context, _payload) =>
      //   context.target?.type === 'processDocument' ? coldDriver : hotDriver,
    }),
  };
}
// @@@SNIPEND

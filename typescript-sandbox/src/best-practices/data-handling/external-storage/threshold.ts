import { DataConverter, ExternalStorage } from '@temporalio/common';
import { FileSystemStorageDriver } from './file-system-storage-driver';
import { STORAGE_ROOT } from './data-converter';

export function createAlwaysExternalDataConverter(rootDir: string = STORAGE_ROOT): DataConverter {
  const driver = new FileSystemStorageDriver({ rootDir });

  // @@@SNIPSTART typescript-best-practices-external-storage-threshold
  const dataConverter = {
    externalStorage: new ExternalStorage({
      drivers: [driver],
      payloadSizeThreshold: 0,
    }),
  };
  // @@@SNIPEND
  return dataConverter;
}

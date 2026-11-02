import { driver } from './s3-driver';

// @@@SNIPSTART typescript-best-practices-external-storage-setup
import { Client, Connection } from '@temporalio/client';
import { ExternalStorage } from '@temporalio/common';
import { Worker } from '@temporalio/worker';

export async function createClientAndWorker() {
  const dataConverter = {
    externalStorage: new ExternalStorage({ drivers: [driver] }),
  };

  const connection = await Connection.connect();
  const client = new Client({ connection, dataConverter });

  const worker = await Worker.create({
    workflowsPath: require.resolve('./workflows'),
    taskQueue: 'my-task-queue',
    dataConverter,
  });

  return { client, worker };
}
// @@@SNIPEND

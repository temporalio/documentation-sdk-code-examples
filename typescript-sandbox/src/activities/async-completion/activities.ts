// @@@SNIPSTART typescript-async-activity-complete
import { CompleteAsyncError, activityInfo } from '@temporalio/activity';
import { Client, Connection } from '@temporalio/client';
import { loadClientConnectConfig } from '@temporalio/envconfig';

export async function doSomethingAsync(): Promise<string> {
  const taskToken = activityInfo().taskToken;
  setTimeout(() => void doSomeWork(taskToken), 1000);
  throw new CompleteAsyncError();
}

// this work could be done in a different process or on a different machine
async function doSomeWork(taskToken: Uint8Array): Promise<void> {
  const config = loadClientConnectConfig();
  const connection = await Connection.connect(config.connectionOptions);
  try {
    const client = new Client({ connection, namespace: config.namespace });
    // does some work...
    await client.activity.complete(taskToken, "Job's done!");
  } finally {
    await connection.close();
  }
}
// @@@SNIPEND

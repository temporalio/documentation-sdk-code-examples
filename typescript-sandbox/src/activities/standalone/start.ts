import { Client, Connection } from '@temporalio/client';
import { loadClientConnectConfig } from '@temporalio/envconfig';
import { nanoid } from 'nanoid';
import * as activities from './quickstart/activities';

export async function run(): Promise<void> {
  const config = loadClientConnectConfig();
  const connection = await Connection.connect(config.connectionOptions);
  const client = new Client({ connection, namespace: config.namespace });
  const activitiesClient = client.activity.typed<typeof activities>();

  try {
    const activityOptions = {
      taskQueue: 'hello-standalone-activities',
      startToCloseTimeout: '10s' as const,
    };
    // In practice, use a meaningful business identifier, like customer or transaction identifier
    const activityId = nanoid();

    // @@@SNIPSTART typescript-standalone-activities-start
    const handle = await activitiesClient.start('greet', {
      ...activityOptions,
      id: activityId,
      args: ['Temporal'],
    });
    // @@@SNIPEND

    // @@@SNIPSTART typescript-standalone-activities-result
    console.log(await handle.result()); // Hello, Temporal!
    // @@@SNIPEND

    // @@@SNIPSTART typescript-standalone-activities-get-handle
    const newHandle = client.activity.getHandle<string>(activityId);
    // @@@SNIPEND
    console.log(await newHandle.result()); // Hello, Temporal!
  } finally {
    await connection.close();
  }
}

if (require.main === module) {
  run().catch((err) => {
    console.error(err);
    process.exit(1);
  });
}

// @@@SNIPSTART typescript-standalone-activities-quickstart-client
import { Connection, Client, ActivityExecutionFailedError } from '@temporalio/client';
import { loadClientConnectConfig } from '@temporalio/envconfig';
import * as activities from './activities';
import { nanoid } from 'nanoid';

export async function run(): Promise<void> {
  const config = loadClientConnectConfig();
  const connection = await Connection.connect(config.connectionOptions);
  const client = new Client({ connection, namespace: config.namespace });

  const activitiesClient = client.activity.typed<typeof activities>();
  // @@@SNIPEND

  try {
    // @@@SNIPSTART typescript-standalone-activities-quickstart-execute-typed
    const taskQueue = 'hello-standalone-activities';
    const activityOptions = {
      taskQueue,
      startToCloseTimeout: '10s' as const,
    };

    // In practice, use a meaningful business identifier, like customer or transaction identifier
    const activityId = nanoid();

    const result = await activitiesClient.execute('greet', {
      ...activityOptions,
      id: activityId,
      args: ['World'],
    });
    // @@@SNIPEND
    console.log(result); // Hello, World!

    // @@@SNIPSTART typescript-standalone-activities-quickstart-execute-untyped
    try {
      await client.activity.execute('greet', {
        ...activityOptions,
        id: activityId + '-untyped',
        args: [1],
      });
    } catch (err) {
      if (err instanceof ActivityExecutionFailedError) {
        console.log('Oops!', err.cause?.message); // Oops! name must be a string
      }
    }
    // @@@SNIPEND
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

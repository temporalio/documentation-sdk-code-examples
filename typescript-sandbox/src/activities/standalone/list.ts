import { Client, Connection } from '@temporalio/client';
import { loadClientConnectConfig } from '@temporalio/envconfig';

export async function run(): Promise<void> {
  const config = loadClientConnectConfig();
  const connection = await Connection.connect(config.connectionOptions);
  const client = new Client({ connection, namespace: config.namespace });

  try {
    // @@@SNIPSTART typescript-standalone-activities-list
    const query = 'TaskQueue="hello-standalone-activities"';

    for await (const a of client.activity.list(query)) {
      console.log(
        `${a.activityId} | ${a.activityRunId} | ${a.activityType} | ${a.status} | ${a.closeTime?.toISOString()}`,
      );
    }
    // @@@SNIPEND

    // @@@SNIPSTART typescript-standalone-activities-count
    const { count } = await client.activity.count(query);
    console.log(`Total activities: ${count}`);
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

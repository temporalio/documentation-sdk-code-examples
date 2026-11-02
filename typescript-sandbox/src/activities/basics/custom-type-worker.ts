import { NativeConnection } from '@temporalio/worker';

// @@@SNIPSTART typescript-activity-basics-custom-type
import { Worker } from '@temporalio/worker';
import { greet } from '../../activities';

export async function createCustomTypeWorker(connection: NativeConnection): Promise<Worker> {
  return await Worker.create({
    connection,
    workflowsPath: require.resolve('./workflows'),
    taskQueue: 'snippets',
    activities: {
      activityFoo: greet,
    },
  });
}
// @@@SNIPEND

async function run() {
  const connection = await NativeConnection.connect({ address: 'localhost:7233' });
  try {
    const worker = await createCustomTypeWorker(connection);
    await worker.run();
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

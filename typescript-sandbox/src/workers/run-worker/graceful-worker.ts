import { NativeConnection, Worker } from '@temporalio/worker';
import { loadClientConnectConfig } from '@temporalio/envconfig';
import * as activities from './activities';

async function run() {
  const connection = await NativeConnection.connect(loadClientConnectConfig().connectionOptions);
  try {
    // @@@SNIPSTART typescript-run-worker-graceful-shutdown
    const worker = await Worker.create({
      connection,
      taskQueue: 'my-task-queue',
      workflowsPath: require.resolve('./workflows'),
      activities,
      shutdownGraceTime: '30s',
    });

    await worker.run();
    // @@@SNIPEND
    console.log(`Worker state: ${worker.getState()}`);
  } finally {
    await connection.close();
  }
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});

import { NativeConnection, Worker } from '@temporalio/worker';
import { loadClientConnectConfig } from '@temporalio/envconfig';
import * as activities from './activities';

async function run() {
  const connection = await NativeConnection.connect(loadClientConnectConfig().connectionOptions);
  try {
    // @@@SNIPSTART typescript-run-worker-versioned
    const worker = await Worker.create({
      connection,
      taskQueue: 'my-task-queue',
      workflowsPath: require.resolve('./workflows'),
      activities,
      workerDeploymentOptions: {
        version: { deploymentName: 'my-app', buildId: '1.0' },
        useWorkerVersioning: true,
        defaultVersioningBehavior: 'PINNED',
      },
    });
    // @@@SNIPEND
    await worker.run();
  } finally {
    await connection.close();
  }
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});

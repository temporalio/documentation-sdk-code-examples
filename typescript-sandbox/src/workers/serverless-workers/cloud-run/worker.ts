// @@@SNIPSTART typescript-cloud-run-worker
import { NativeConnection, Worker } from '@temporalio/worker';
import * as activities from './activities';

async function run() {
  const connection = await NativeConnection.connect({
    address: process.env.TEMPORAL_ADDRESS,
    apiKey: process.env.TEMPORAL_API_KEY,
    tls: true,
  });

  const worker = await Worker.create({
    connection,
    namespace: process.env.TEMPORAL_NAMESPACE!,
    taskQueue: process.env.TEMPORAL_TASK_QUEUE!,
    workflowsPath: require.resolve('./workflows'),
    activities,
    workerDeploymentOptions: {
      version: { deploymentName: 'my-app', buildId: 'build-1' },
      useWorkerVersioning: true,
      defaultVersioningBehavior: 'PINNED',
    },
  });

  await worker.run();
}
// @@@SNIPEND

run().catch((err) => {
  console.error(err);
  process.exit(1);
});

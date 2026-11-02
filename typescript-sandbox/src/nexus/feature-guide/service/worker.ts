// @@@SNIPSTART typescript-nexus-feature-guide-service-worker
import { Worker, NativeConnection } from '@temporalio/worker';
import { loadClientConnectConfig } from '@temporalio/envconfig';
import { helloServiceHandler } from './handler';
import { helloArgsServiceHandler } from './multiple-args';

async function run() {
  const connection = await NativeConnection.connect(loadClientConnectConfig().connectionOptions);
  try {
    const namespace = 'my-target-namespace';
    const serviceTaskQueue = 'my-handler-task-queue';
    const worker = await Worker.create({
      connection,
      namespace,
      taskQueue: serviceTaskQueue,
      workflowsPath: require.resolve('./workflows'),
      nexusServices: [helloServiceHandler, helloArgsServiceHandler],
    });

    await worker.run();
  } finally {
    await connection.close();
  }
}
// @@@SNIPEND

run().catch((err) => {
  console.error(err);
  process.exit(1);
});

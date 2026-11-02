import { NativeConnection, Worker } from '@temporalio/worker';
import { loadClientConnectConfig } from '@temporalio/envconfig';

async function run() {
  const connection = await NativeConnection.connect(loadClientConnectConfig().connectionOptions);
  try {
    const namespace = 'my-caller-namespace';
    const callerTaskQueue = 'nexus-hello-caller-task-queue';
    const worker = await Worker.create({
      connection,
      namespace,
      taskQueue: callerTaskQueue,
      workflowsPath: require.resolve('./workflows'),
    });

    await worker.run();
  } finally {
    await connection.close();
  }
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});

import { NativeConnection, Worker } from '@temporalio/worker';

async function run() {
  const connection = await NativeConnection.connect({ address: 'localhost:7233' });
  try {
    // @@@SNIPSTART typescript-versioning-cutover-worker
    const worker = await Worker.create({
      workflowsPath: require.resolve('./workflows'),
      // other configurations
      connection,
      taskQueue: 'pizza-tasks',
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

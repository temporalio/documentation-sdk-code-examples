import { NativeConnection, Worker } from '@temporalio/worker';
import { loadClientConnectConfig } from '@temporalio/envconfig';
import { getDataConverter } from './data-converter';

async function run() {
  const connection = await NativeConnection.connect(loadClientConnectConfig().connectionOptions);
  // @@@SNIPSTART typescript-data-encryption-worker
  const worker = await Worker.create({
    connection,
    workflowsPath: require.resolve('./workflows'),
    taskQueue: 'encryption',
    dataConverter: await getDataConverter(),
  });
  // @@@SNIPEND

  await worker.run();
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});

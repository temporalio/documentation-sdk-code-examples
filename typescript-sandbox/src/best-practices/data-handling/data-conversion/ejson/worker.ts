import { NativeConnection, Worker } from '@temporalio/worker';
import { loadClientConnectConfig } from '@temporalio/envconfig';

async function run() {
  const connection = await NativeConnection.connect(loadClientConnectConfig().connectionOptions);
  // @@@SNIPSTART typescript-data-conversion-ejson-worker
  const worker = await Worker.create({
    connection,
    workflowsPath: require.resolve('./workflows'),
    taskQueue: 'ejson',
    dataConverter: { payloadConverterPath: require.resolve('./payload-converter') },
  });
  // @@@SNIPEND

  await worker.run();
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});

// @@@SNIPSTART typescript-run-worker-create
import { NativeConnection, Worker } from '@temporalio/worker';
import { loadClientConnectConfig } from '@temporalio/envconfig';
import * as activities from './activities';

async function run() {
  const connection = await NativeConnection.connect(loadClientConnectConfig().connectionOptions);
  const worker = await Worker.create({
    connection,
    workflowsPath: require.resolve('./workflows'),
    taskQueue: 'snippets',
    activities,
  });

  await worker.run();
}
// @@@SNIPEND

run().catch((err) => {
  console.error(err);
  process.exit(1);
});

// @@@SNIPSTART typescript-client-worker-connect-env
import { NativeConnection, Worker } from '@temporalio/worker';
import { loadClientConnectConfig } from '@temporalio/envconfig';
import * as activities from './activities';

async function main() {
  const config = loadClientConnectConfig();

  const connection = await NativeConnection.connect(config.connectionOptions);

  const worker = await Worker.create({
    connection,
    namespace: config.namespace,
    taskQueue: 'your-task-queue',
    workflowsPath: require.resolve('./workflows'),
    activities,
  });
  await worker.run();
}
// @@@SNIPEND

main().catch((err) => {
  console.error(err);
  process.exit(1);
});

// @@@SNIPSTART typescript-client-worker-connect-config-file
import { NativeConnection, Worker } from '@temporalio/worker';
import { loadClientConnectConfig } from '@temporalio/envconfig';
import { resolve } from 'path';
import * as activities from './activities';

async function main() {
  const configFile = resolve(__dirname, 'config.toml');
  const profileName = 'staging';

  // Load the 'staging' profile.
  const config = loadClientConnectConfig({
    profile: profileName,
    configSource: { path: configFile },
  });

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

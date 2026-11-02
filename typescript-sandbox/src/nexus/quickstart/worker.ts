// @@@SNIPSTART typescript-nexus-quickstart-worker
import { NativeConnection, Worker } from '@temporalio/worker';
import { loadClientConnectConfig } from '@temporalio/envconfig';
import * as activities from './activities';
import { sayHelloHandler } from './handler';

async function run() {
  // Connects to localhost:7233 unless your environment configuration says otherwise
  const connection = await NativeConnection.connect(loadClientConnectConfig().connectionOptions);
  try {
    const worker = await Worker.create({
      connection,
      namespace: 'default',
      taskQueue: 'hello-world',
      workflowsPath: require.resolve('./workflows'),
      activities,
      nexusServices: [sayHelloHandler],
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
// @@@SNIPEND

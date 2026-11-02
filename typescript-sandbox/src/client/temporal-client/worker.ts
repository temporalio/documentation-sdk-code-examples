import { NativeConnection } from '@temporalio/worker';

// @@@SNIPSTART typescript-client-task-queue-worker
import { Worker } from '@temporalio/worker';
import * as activities from './activities';

export async function run(connection?: NativeConnection): Promise<void> {
  // Step 1: Register Workflows and Activities with the Worker and connect to
  // the Temporal server.
  const worker = await Worker.create({
    connection,
    workflowsPath: require.resolve('./workflows'),
    activities,
    taskQueue: 'hello-world',
  });
  // Without a connection, the Worker connects to localhost:7233.
  // Customize the Worker by passing more options to create():
  // https://typescript.temporal.io/api/classes/worker.Worker
  // If you need to configure server connection parameters, see docs:
  // /develop/typescript/client/temporal-client#connect-to-temporal-service-from-a-worker

  // Step 2: Start accepting tasks on the `hello-world` queue
  await worker.run();
}
// @@@SNIPEND

export async function createTaskQueueWorker(connection: NativeConnection): Promise<Worker> {
  // @@@SNIPSTART typescript-client-task-queue-worker-options
  const worker = await Worker.create({
    connection,
    workflowsPath: require.resolve('./workflows'),
    // imported elsewhere
    activities,
    taskQueue: 'your-task-queue',
  });
  // @@@SNIPEND
  return worker;
}

if (require.main === module) {
  run().catch((err) => {
    console.error(err);
    process.exit(1);
  });
}

// @@@SNIPSTART typescript-nexus-quickstart-caller-starter
import { randomUUID } from 'crypto';
import { Connection, Client } from '@temporalio/client';
import { loadClientConnectConfig } from '@temporalio/envconfig';
import { NativeConnection, Worker } from '@temporalio/worker';
import { callerWorkflow } from './workflows';

const CALLER_TASK_QUEUE = 'my-caller-task-queue';
const NAMESPACE = 'my-caller-namespace';

async function main() {
  // Connects to localhost:7233 unless your environment configuration says otherwise
  const { connectionOptions } = loadClientConnectConfig();
  const clientConnection = await Connection.connect(connectionOptions);
  const client = new Client({
    connection: clientConnection,
    namespace: NAMESPACE,
  });

  const workerConnection = await NativeConnection.connect(connectionOptions);
  try {
    const worker = await Worker.create({
      connection: workerConnection,
      namespace: NAMESPACE,
      taskQueue: CALLER_TASK_QUEUE,
      workflowsPath: require.resolve('./workflows'),
    });

    await worker.runUntil(async () => {
      const result = await client.workflow.execute(callerWorkflow, {
        args: ['Temporal'],
        workflowId: `caller-workflow-${randomUUID()}`,
        taskQueue: CALLER_TASK_QUEUE,
      });
      console.log('Workflow result:', result);
    });
  } finally {
    await workerConnection.close();
    await clientConnection.close();
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
// @@@SNIPEND

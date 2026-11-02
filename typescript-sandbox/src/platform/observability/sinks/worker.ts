// @@@SNIPSTART typescript-observability-sink-worker
import { InjectedSinks, NativeConnection, Worker } from '@temporalio/worker';
import { loadClientConnectConfig } from '@temporalio/envconfig';
import { MySinks } from './workflows';

async function main() {
  const sinks: InjectedSinks<MySinks> = {
    alerter: {
      alert: {
        fn(workflowInfo, message) {
          console.log('sending SMS alert!', {
            workflowId: workflowInfo.workflowId,
            workflowRunId: workflowInfo.runId,
            message,
          });
        },
        callDuringReplay: false, // The default
      },
    },
  };
  const connection = await NativeConnection.connect(loadClientConnectConfig().connectionOptions);
  const worker = await Worker.create({
    connection,
    workflowsPath: require.resolve('./workflows'),
    taskQueue: 'sinks',
    sinks,
  });
  await worker.run();
  await connection.close();
  console.log('Worker gracefully shutdown');
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
// @@@SNIPEND

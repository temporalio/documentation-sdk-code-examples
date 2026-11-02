// @@@SNIPSTART typescript-interceptors-register-nexus
import { NativeConnection, Worker } from '@temporalio/worker';
import { NexusOperationLogInterceptor } from './nexus-interceptors';
import { echoServiceHandler } from './nexus-service';

export async function createNexusWorker(connection: NativeConnection, taskQueue: string): Promise<Worker> {
  const worker = await Worker.create({
    connection,
    taskQueue,
    workflowsPath: require.resolve('./workflows'),
    nexusServices: [echoServiceHandler],
    interceptors: {
      nexus: [
        (_ctx) => ({
          inbound: new NexusOperationLogInterceptor(),
        }),
      ],
    },
  });
  return worker;
}
// @@@SNIPEND

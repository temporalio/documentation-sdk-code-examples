import { NativeConnection, Worker } from '@temporalio/worker';
import * as activities from './activities';

export async function createInterceptorWorker(connection: NativeConnection, taskQueue: string): Promise<Worker> {
  // @@@SNIPSTART typescript-interceptors-register-workflow
  const worker = await Worker.create({
    connection,
    taskQueue,
    activities,
    workflowsPath: require.resolve('./workflows'),
    interceptors: {
      workflowModules: [require.resolve('./workflow-interceptors')],
    },
  });
  // @@@SNIPEND
  return worker;
}

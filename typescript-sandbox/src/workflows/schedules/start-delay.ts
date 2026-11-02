import { Client, WorkflowHandle } from '@temporalio/client';
import { someWorkflow } from './workflows';

export async function startDelayedWorkflow(
  client: Client,
  taskQueue: string,
  workflowId: string,
): Promise<WorkflowHandle<typeof someWorkflow>> {
  // @@@SNIPSTART typescript-schedules-start-delay
  const handle = await client.workflow.start(someWorkflow, {
    taskQueue,
    workflowId,
    startDelay: '2 hours',
  });
  // @@@SNIPEND
  return handle;
}

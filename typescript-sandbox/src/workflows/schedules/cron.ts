import { Client, WorkflowHandle } from '@temporalio/client';
import { scheduledWorkflow } from './workflows';

export async function startCronWorkflow(
  client: Client,
  taskQueue: string,
  workflowId: string,
): Promise<WorkflowHandle<typeof scheduledWorkflow>> {
  // @@@SNIPSTART typescript-schedules-cron
  const handle = await client.workflow.start(scheduledWorkflow, {
    taskQueue,
    workflowId,
    cronSchedule: '* * * * *', // start every minute
  });
  // @@@SNIPEND
  return handle;
}

import { Client, WorkflowHandle } from '@temporalio/client';
import { example } from './workflows';

export async function startWithTimeouts(
  client: Client,
  taskQueue: string,
  workflowId: string,
): Promise<WorkflowHandle<typeof example>> {
  // @@@SNIPSTART typescript-workflow-timeouts-start-with-timeouts
  await client.workflow.start(example, {
    taskQueue,
    workflowId,
    // Set Workflow Timeout duration
    workflowExecutionTimeout: '1 day',
    // workflowRunTimeout: '1 minute',
    // workflowTaskTimeout: '30 seconds',
  });
  // @@@SNIPEND
  return client.workflow.getHandle(workflowId);
}

export async function startWithRetryPolicy(
  client: Client,
  taskQueue: string,
  workflowId: string,
): Promise<WorkflowHandle<typeof example>> {
  // @@@SNIPSTART typescript-workflow-timeouts-start-with-retry
  const handle = await client.workflow.start(example, {
    taskQueue,
    workflowId,
    retry: {
      maximumAttempts: 3,
      maximumInterval: '30 seconds',
    },
  });
  // @@@SNIPEND
  return handle;
}

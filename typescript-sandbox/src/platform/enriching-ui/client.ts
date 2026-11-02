import { Client, WorkflowHandle } from '@temporalio/client';
import { yourWorkflow } from './current-details';

export async function startWithSummary(client: Client): Promise<WorkflowHandle<typeof yourWorkflow>> {
  // @@@SNIPSTART typescript-enriching-ui-start
  // Start a workflow with static summary and details
  const handle = await client.workflow.start(yourWorkflow, {
    args: ['workflow input'],
    taskQueue: 'your-task-queue',
    workflowId: 'your-workflow-id',
    staticSummary: 'Order processing for customer #12345',
    staticDetails: 'Processing premium order with expedited shipping',
  });
  // @@@SNIPEND
  return handle;
}

export async function executeWithSummary(client: Client): Promise<string> {
  // @@@SNIPSTART typescript-enriching-ui-execute
  const result = await client.workflow.execute(yourWorkflow, {
    args: ['workflow input'],
    taskQueue: 'your-task-queue',
    workflowId: 'your-workflow-id',
    staticSummary: 'Order processing for customer #12345',
    staticDetails: 'Processing premium order with expedited shipping',
  });
  // @@@SNIPEND
  return result;
}

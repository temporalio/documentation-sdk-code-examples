import { Client, WorkflowFailedError } from '@temporalio/client';
import { example, yourWorkflow } from './workflows';

export async function startWorkflow(client: Client): Promise<string> {
  // @@@SNIPSTART typescript-client-start-workflow
  const handle = await client.workflow.start(example, {
    workflowId: 'your-workflow-id',
    taskQueue: 'your-task-queue',
    args: ['argument01', 'argument02', 'argument03'], // this is typechecked against workflowFn's args
  });
  const result = await handle.result();
  // @@@SNIPEND
  return result;
}

export async function executeWithTaskQueue(client: Client): Promise<string> {
  // @@@SNIPSTART typescript-client-execute-task-queue
  const result = await client.workflow.execute(yourWorkflow, {
    // required
    taskQueue: 'your-task-queue',
    // required
    workflowId: 'your-workflow-id',
  });
  // @@@SNIPEND
  return result;
}

export async function startWithWorkflowId(client: Client): Promise<string> {
  // @@@SNIPSTART typescript-client-workflow-id
  const handle = await client.workflow.start(example, {
    workflowId: 'yourWorkflowId',
    taskQueue: 'yourTaskQueue',
    args: ['your', 'arg', 'uments'],
  });
  // @@@SNIPEND
  return handle.workflowId;
}

export async function getResult(client: Client, workflowId: string): Promise<unknown> {
  // @@@SNIPSTART typescript-client-get-result
  const handle = client.workflow.getHandle(workflowId);
  const result = await handle.result();
  // @@@SNIPEND
  return result;
}

export async function getResultOrThrow(client: Client, workflowId: string): Promise<unknown> {
  // @@@SNIPSTART typescript-client-workflow-failed-error
  const handle = client.workflow.getHandle(workflowId);
  try {
    const result = await handle.result();
    return result;
  } catch (err) {
    if (err instanceof WorkflowFailedError) {
      throw new Error('Temporal workflow failed: ' + workflowId, {
        cause: err,
      });
    } else {
      throw new Error('error from Temporal workflow ' + workflowId, {
        cause: err,
      });
    }
  }
  // @@@SNIPEND
}

// @@@SNIPSTART typescript-child-workflows-external-handle
import { getExternalWorkflowHandle } from '@temporalio/workflow';

export async function cancelWorkflow(workflowId: string): Promise<void> {
  const handle = getExternalWorkflowHandle(workflowId); // sync function, not async
  await handle.cancel();
}
// @@@SNIPEND

import { condition } from '@temporalio/workflow';

export async function longRunningWorkflow(): Promise<void> {
  await condition(() => false);
}

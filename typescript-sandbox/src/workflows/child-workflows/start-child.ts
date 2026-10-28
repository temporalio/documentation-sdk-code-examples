// @@@SNIPSTART typescript-child-workflows-start-child
import { startChild } from '@temporalio/workflow';

export async function parentWorkflow(name: string): Promise<string> {
  const childHandle = await startChild(childWorkflow, {
    args: [name],
    // workflowId, // add business-meaningful workflow id here
    // // regular workflow options apply here, with two additions (defaults shown):
    // cancellationType: ChildWorkflowCancellationType.WAIT_CANCELLATION_COMPLETED,
    // parentClosePolicy: ParentClosePolicy.TERMINATE
  });
  // you can use childHandle to signal or get result here
  await childHandle.signal(anySignal);
  const result = await childHandle.result();
  // you can use childHandle to signal, query, cancel, terminate, or get result here
  return result;
}
// @@@SNIPEND

import { condition, defineSignal, setHandler } from '@temporalio/workflow';

export const anySignal = defineSignal('anySignal');

export async function childWorkflow(name: string): Promise<string> {
  let signaled = false;
  setHandler(anySignal, () => {
    signaled = true;
  });
  await condition(() => signaled);
  return `I am a child named ${name}`;
}

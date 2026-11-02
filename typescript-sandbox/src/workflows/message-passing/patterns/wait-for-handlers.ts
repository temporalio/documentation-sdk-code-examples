import * as wf from '@temporalio/workflow';
import type * as activities from './activities';
import { MyWorkflowOutput, finishSignal, mySignal } from './shared';

const { myActivity } = wf.proxyActivities<typeof activities>({
  startToCloseTimeout: '1 minute',
});

// @@@SNIPSTART typescript-message-passing-wait-for-handlers
export async function myWorkflow(): Promise<MyWorkflowOutput> {
  const workflowOutput = { x: 0, y: 0 };
  let finished = false;
  wf.setHandler(mySignal, async () => {
    const data = await myActivity();
    workflowOutput.x = data.x;
    workflowOutput.y = data.y;
  });
  wf.setHandler(finishSignal, () => void (finished = true));

  await wf.condition(() => finished);
  // 👉 Wait for any mySignal handler that is still running its Activity
  await wf.condition(wf.allHandlersFinished);
  return workflowOutput;
}
// @@@SNIPEND

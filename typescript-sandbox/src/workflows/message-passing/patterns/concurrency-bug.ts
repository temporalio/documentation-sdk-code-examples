import * as wf from '@temporalio/workflow';
import type * as activities from './activities';
import { MyWorkflowOutput, finishSignal, mySignal } from './shared';

const { myActivity } = wf.proxyActivities<typeof activities>({
  startToCloseTimeout: '1 minute',
});

// @@@SNIPSTART typescript-message-passing-concurrency-bug
export async function myWorkflow(): Promise<MyWorkflowOutput> {
  let x = 0;
  let y = 0;
  wf.setHandler(mySignal, async () => {
    const data = await myActivity();
    x = data.x;

    // 🐛🐛 Bug!! If multiple instances of this handler are executing
    // concurrently, then there may be times when the Workflow has x from one
    // Activity execution and y from another.
    await wf.sleep(500); // or await anything else

    y = data.y;
  });
  // @@@SNIPEND

  let finished = false;
  wf.setHandler(finishSignal, () => void (finished = true));
  await wf.condition(() => finished);
  await wf.condition(wf.allHandlersFinished);
  return { x, y };
}

import * as wf from '@temporalio/workflow';
import type * as activities from './activities';
import { MyWorkflowOutput, finishSignal, mySignal } from './shared';

const { myActivity } = wf.proxyActivities<typeof activities>({
  startToCloseTimeout: '1 minute',
});

// @@@SNIPSTART typescript-message-passing-concurrency-lock
import { Mutex } from 'async-mutex'; // https://github.com/DirtyHairy/async-mutex

export async function myWorkflow(): Promise<MyWorkflowOutput> {
  let x = 0;
  let y = 0;
  const lock = new Mutex();

  wf.setHandler(mySignal, async () => {
    await lock.runExclusive(async () => {
      const data = await myActivity();
      x = data.x;

      // ✅ OK: node's event loop may switch now to a different handler
      // execution, or to the main workflow function, but no other execution of
      // this handler can run until this execution finishes.
      await wf.sleep(500); // or await anything else

      y = data.y;
    });
  });
  // @@@SNIPEND

  let finished = false;
  wf.setHandler(finishSignal, () => void (finished = true));
  await wf.condition(() => finished);
  await wf.condition(wf.allHandlersFinished);
  return { x, y };
}

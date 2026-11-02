import {
  CancellationScope,
  condition,
  continueAsNew,
  defineSignal,
  isCancellation,
  proxyActivities,
  setHandler,
} from '@temporalio/workflow';
import type * as activities from './activities';

const { setup, runAnActivityOrChildWorkflow, cleanup } = proxyActivities<typeof activities>({
  startToCloseTimeout: '1 minute',
});

// @@@SNIPSTART typescript-entity-pattern-workflow
interface Input {
  /* Define your Workflow input type here */
  entityId: string;
}
interface Update {
  /* Define your Workflow update type here */
  value: number;
}

export const updateSignal = defineSignal<[Update]>('update');

const MAX_ITERATIONS = 1;

export async function entityWorkflow(input: Input, isNew = true): Promise<void> {
  try {
    const pendingUpdates = Array<Update>();
    setHandler(updateSignal, (updateCommand) => {
      pendingUpdates.push(updateCommand);
    });

    if (isNew) {
      await setup(input);
    }

    for (let iteration = 1; iteration <= MAX_ITERATIONS; ++iteration) {
      // Ensure that we don't block the Workflow Execution forever waiting
      // for updates, which means that it will eventually Continue-As-New
      // even if it does not receive updates.
      await condition(() => pendingUpdates.length > 0, '1 day');

      while (pendingUpdates.length) {
        const update = pendingUpdates.shift()!;
        await runAnActivityOrChildWorkflow(update);
      }
    }
  } catch (err) {
    if (isCancellation(err)) {
      await CancellationScope.nonCancellable(async () => {
        await cleanup();
      });
    }
    throw err;
  }
  await continueAsNew<typeof entityWorkflow>(input, false);
}
// @@@SNIPEND

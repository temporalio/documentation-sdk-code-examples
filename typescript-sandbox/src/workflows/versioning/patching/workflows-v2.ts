import { proxyActivities, sleep } from '@temporalio/workflow';
import type * as activities from './activities';

const { activityA, activityB, activityThatMustRunAfterA } = proxyActivities<typeof activities>({
  startToCloseTimeout: '1 minute',
});

// @@@SNIPSTART typescript-versioning-patching-v2
// v2
import { patched } from '@temporalio/workflow';
export async function myWorkflow(): Promise<void> {
  if (patched('my-change-id')) {
    await activityB();
    await sleep('1 days');
  } else {
    await activityA();
    await sleep('1 days');
    await activityThatMustRunAfterA();
  }
}
// @@@SNIPEND

import { proxyActivities, sleep } from '@temporalio/workflow';
import type * as activities from './activities';

const { activityB } = proxyActivities<typeof activities>({
  startToCloseTimeout: '1 minute',
});

// @@@SNIPSTART typescript-versioning-patching-v3
// v3
import { deprecatePatch } from '@temporalio/workflow';

export async function myWorkflow(): Promise<void> {
  deprecatePatch('my-change-id');
  await activityB();
  await sleep('1 days');
}
// @@@SNIPEND

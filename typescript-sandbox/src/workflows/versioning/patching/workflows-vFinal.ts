import { proxyActivities, sleep } from '@temporalio/workflow';
import type * as activities from './activities';

const { activityB } = proxyActivities<typeof activities>({
  startToCloseTimeout: '1 minute',
});

// @@@SNIPSTART typescript-versioning-patching-final
// vFinal
export async function myWorkflow(): Promise<void> {
  await activityB();
  await sleep('1 days');
}
// @@@SNIPEND

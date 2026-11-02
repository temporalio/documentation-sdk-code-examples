import { proxyActivities } from '@temporalio/workflow';
import type * as activities from './activities';

// @@@SNIPSTART typescript-activity-timeouts-options
const { myActivity } = proxyActivities<typeof activities>({
  scheduleToCloseTimeout: '5m',
  // startToCloseTimeout: "30s", // recommended
  // scheduleToStartTimeout: "60s",
});
// @@@SNIPEND

export async function timeoutsWorkflow(): Promise<void> {
  await myActivity();
}

import { proxyActivities } from '@temporalio/workflow';
import type * as activities from './activities';

// @@@SNIPSTART typescript-activity-timeouts-heartbeat-timeout
const { myLongRunningActivity } = proxyActivities<typeof activities>({
  startToCloseTimeout: '1h',
  heartbeatTimeout: '30s',
});
// @@@SNIPEND

export async function heartbeatTimeoutWorkflow(): Promise<void> {
  await myLongRunningActivity();
}

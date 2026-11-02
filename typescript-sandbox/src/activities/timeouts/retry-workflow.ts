import { proxyActivities } from '@temporalio/workflow';
import type * as activities from './activities';

// @@@SNIPSTART typescript-activity-timeouts-retry-policy
const { chargeCard } = proxyActivities<typeof activities>({
  startToCloseTimeout: '30s',
  retry: {
    initialInterval: '10s',
    maximumAttempts: 5,
  },
});
// @@@SNIPEND

export async function retryWorkflow(): Promise<void> {
  await chargeCard();
}

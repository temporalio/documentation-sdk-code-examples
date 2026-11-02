// @@@SNIPSTART typescript-cancellation-cancellation-type
import { ActivityCancellationType, proxyActivities } from '@temporalio/workflow';
import type * as activities from './activities';

const { processOrder } = proxyActivities<typeof activities>({
  startToCloseTimeout: '10 minutes',
  heartbeatTimeout: '30 seconds',
  cancellationType: ActivityCancellationType.WAIT_CANCELLATION_COMPLETED,
});
// @@@SNIPEND

export async function orderWorkflow(orderId: string): Promise<string> {
  return await processOrder(orderId);
}

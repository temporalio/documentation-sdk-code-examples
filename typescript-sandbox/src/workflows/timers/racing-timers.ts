import { proxyActivities, sleep } from '@temporalio/workflow';
import type * as activities from './activities';

const { processOrder, sendNotificationEmail } = proxyActivities<typeof activities>({
  startToCloseTimeout: '1 minute',
});

export interface ProcessOrderOptions {
  orderProcessingMS: number;
  sendDelayedEmailTimeoutMS: number;
}

// @@@SNIPSTART typescript-timers-racing-timers
export async function processOrderWorkflow({
  orderProcessingMS,
  sendDelayedEmailTimeoutMS,
}: ProcessOrderOptions): Promise<void> {
  let processing = true;
  const processOrderPromise = processOrder(orderProcessingMS).then(() => {
    processing = false;
  });

  await Promise.race([processOrderPromise, sleep(sendDelayedEmailTimeoutMS)]);

  if (processing) {
    await sendNotificationEmail();
    await processOrderPromise;
  }
}
// @@@SNIPEND

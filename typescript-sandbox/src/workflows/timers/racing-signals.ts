import { proxyActivities } from '@temporalio/workflow';
import type * as activities from './activities';

const { sendReminderEmail } = proxyActivities<typeof activities>({
  startToCloseTimeout: '1 minute',
});

// @@@SNIPSTART typescript-timers-racing-signals
import { defineSignal, setHandler, sleep, Trigger } from '@temporalio/workflow';

const userInteraction = new Trigger<boolean>();
export const completeUserInteraction = defineSignal('completeUserInteraction');

export async function yourWorkflow(userId: string) {
  setHandler(completeUserInteraction, () => userInteraction.resolve(true)); // programmatic resolve
  const userInteracted = await Promise.race([userInteraction, sleep('30 days')]);
  if (!userInteracted) {
    await sendReminderEmail(userId);
  }
}
// @@@SNIPEND

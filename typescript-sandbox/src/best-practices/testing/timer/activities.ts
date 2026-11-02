import { activityInfo, log, sleep } from '@temporalio/activity';

export const createActivities = () => ({
  async processOrder(): Promise<void> {
    // Delay completion to simulate work and show how to race an activity and a timer.
    await sleep(activityInfo().startToCloseTimeoutMs / 2);
    log.info('Order processed');
  },

  async sendNotificationEmail(): Promise<void> {
    log.info('Sending email: order processing is taking longer than expected');
  },
});

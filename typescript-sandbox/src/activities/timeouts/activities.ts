import { ApplicationFailure, activityInfo } from '@temporalio/activity';

export { myActivity } from './heartbeat';
export { myActivity as myLongRunningActivity } from './heartbeat-checkpoint';

export async function chargeCard(): Promise<void> {
  if (activityInfo().attempt === 1) {
    // @@@SNIPSTART typescript-activity-timeouts-next-retry-delay
    throw ApplicationFailure.create({
      message: 'Payment service is unavailable',
      nextRetryDelay: '15s',
    });
    // @@@SNIPEND
  }
}

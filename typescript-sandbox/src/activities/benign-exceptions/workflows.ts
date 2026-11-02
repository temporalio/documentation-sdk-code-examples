import { proxyActivities } from '@temporalio/workflow';
import type * as activities from './activities';

const { myActivity } = proxyActivities<typeof activities>({
  startToCloseTimeout: '1 minute',
  retry: { maximumAttempts: 1 },
});

export async function benignExceptionWorkflow(): Promise<string> {
  return await myActivity();
}

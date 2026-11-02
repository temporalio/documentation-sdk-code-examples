import { proxyActivities } from '@temporalio/workflow';
import type { greet } from '../../activities';

// The Worker registers greet under the Activity Type activityFoo
const { activityFoo } = proxyActivities<{ activityFoo: typeof greet }>({
  startToCloseTimeout: '30 seconds',
});

export async function customActivityTypeWorkflow(name: string): Promise<string> {
  return await activityFoo(name);
}

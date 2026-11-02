import { proxyActivities } from '@temporalio/workflow';
import type * as activities from './activities';

// @@@SNIPSTART typescript-activity-basics-multiple-activities
export async function multipleActivitiesWorkflow(name: string): Promise<string[]> {
  // destructuring multiple activities with the same options
  const { act1, act2, act3 } = proxyActivities<typeof activities>({
    startToCloseTimeout: '30 seconds', // activityOptions
  });
  const first = await act1();
  const [second, third] = await Promise.all([act2(), act3()]);
  return [name, first, second, third];
}
// @@@SNIPEND

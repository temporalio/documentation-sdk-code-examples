import { proxyActivities } from '@temporalio/workflow';

// @@@SNIPSTART typescript-activity-basics-dynamic-reference
export async function dynamicWorkflow(activityName: string, ...args: unknown[]): Promise<unknown> {
  const acts = proxyActivities<Record<string, (...args: unknown[]) => Promise<unknown>>>({
    startToCloseTimeout: '30 seconds', // activityOptions
  });

  // these are equivalent
  await acts.activity1();
  await acts['activity1']();

  // dynamic reference to activities using activityName
  const result = await acts[activityName](...args);
  return result;
}
// @@@SNIPEND

// @@@SNIPSTART typescript-activity-execution-proxy-activities
import { proxyActivities } from '@temporalio/workflow';
// Only import the activity types, not the functions themselves
import type * as activities from './activities';

// Retrieve the Activity Handle by passing in the Activity types and options
const activityHandle = proxyActivities<typeof activities>({
  startToCloseTimeout: '1 minute',
});

// Deconstruct the individual Activity functions from the Activity Handle
const { greet } = activityHandle;

// A workflow that calls an activity
export async function example(name: string): Promise<string> {
  return await greet(name);
}
// @@@SNIPEND

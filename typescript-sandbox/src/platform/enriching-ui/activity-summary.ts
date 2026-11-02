// @@@SNIPSTART typescript-enriching-ui-activity-summary
import { proxyActivities } from '@temporalio/workflow';
import type * as activities from './activities';

const { yourActivity } = proxyActivities<typeof activities>({
  startToCloseTimeout: '10 seconds',
});

export async function yourWorkflow(input: string): Promise<string> {
  // Execute an activity with a summary using executeWithOptions
  const result = await yourActivity.executeWithOptions(
    {
      summary: 'Processing user data',
    },
    [input], // Note: arguments must be passed as an array
  );

  return result;
}
// @@@SNIPEND

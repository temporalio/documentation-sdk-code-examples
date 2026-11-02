// @@@SNIPSTART typescript-enriching-ui-timer-summary
import { sleep } from '@temporalio/workflow';

export async function yourWorkflow(input: string): Promise<string> {
  // Create a timer with a summary
  await sleep('5 minutes', { summary: 'Waiting for payment confirmation' });

  return 'Timer completed';
}
// @@@SNIPEND

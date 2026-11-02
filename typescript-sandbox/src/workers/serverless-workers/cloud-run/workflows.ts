import { proxyActivities } from '@temporalio/workflow';
import type * as activities from './activities';

const { myActivity } = proxyActivities<typeof activities>({
  startToCloseTimeout: '1 minute',
  heartbeatTimeout: '10 seconds',
});

// @@@SNIPSTART typescript-cloud-run-versioning-behavior
import { setWorkflowOptions } from '@temporalio/workflow';

setWorkflowOptions({ versioningBehavior: 'PINNED' }, myWorkflow);
export async function myWorkflow(): Promise<string> {
  return await myActivity(['first', 'second', 'third']);
}
// @@@SNIPEND

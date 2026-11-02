// @@@SNIPSTART typescript-local-activities-proxy
import * as workflow from '@temporalio/workflow';
import type * as activities from './activities';

const { getEnvVar } = workflow.proxyLocalActivities<typeof activities>({
  startToCloseTimeout: '2 seconds',
});

export async function yourWorkflow(): Promise<string | undefined> {
  const someSetting = await getEnvVar('SOME_SETTING');
  // ...
  return someSetting;
}
// @@@SNIPEND

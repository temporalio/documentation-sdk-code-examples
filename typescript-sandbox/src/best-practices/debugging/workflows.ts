// @@@SNIPSTART typescript-debugging-proxy-activities
import { proxyActivities } from '@temporalio/workflow';

import type * as activities from './activities';
const { makeHTTPRequest } = proxyActivities<typeof activities>({
  startToCloseTimeout: '1 minute',
});

export async function yourWorkflow(): Promise<string> {
  return await makeHTTPRequest('https://temporal.io');
}
// @@@SNIPEND

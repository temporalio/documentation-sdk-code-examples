import { proxyActivities } from '@temporalio/workflow';
import type * as activities from '../activities';

const { makeHTTPRequest } = proxyActivities<typeof activities>({ startToCloseTimeout: '1 minute' });

export async function httpWorkflow(): Promise<string> {
  const answer = await makeHTTPRequest();
  return `The answer is ${answer}`;
}

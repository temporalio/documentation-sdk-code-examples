import { proxyActivities } from '@temporalio/workflow';
import type * as activities from './activities';

const { doSomethingAsync } = proxyActivities<typeof activities>({
  startToCloseTimeout: '1 minute',
});

export async function asyncCompletionWorkflow(): Promise<string> {
  return await doSomethingAsync();
}

import { proxyActivities, sleep } from '@temporalio/workflow';
import type * as activities from './activities';

const acts = proxyActivities<typeof activities>({
  startToCloseTimeout: '1 minute',
});
const { complete } = acts;

export async function sleepThenAntipatternWorkflow(): Promise<string> {
  let status = 'pending';
  const somethingElse = acts.somethingElse();

  // @@@SNIPSTART typescript-timers-sleep-then-antipattern
  await Promise.race([
    sleep('5s').then(() => (status = 'timed_out')),
    somethingElse.then(() => (status = 'processed')),
  ]);

  if (status === 'processed') await complete(); // takes more than 5 seconds
  // status = timed_out
  // @@@SNIPEND

  return status;
}

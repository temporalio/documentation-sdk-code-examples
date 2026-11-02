import { heartbeat, sleep } from '@temporalio/activity';

// @@@SNIPSTART typescript-activity-timeouts-heartbeat
export async function myActivity(): Promise<void> {
  for (let progress = 1; progress <= 1000; ++progress) {
    // Do something that takes time
    await sleep('1s');

    heartbeat();
  }
}
// @@@SNIPEND

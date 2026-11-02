import { activityInfo, heartbeat, sleep } from '@temporalio/activity';

// @@@SNIPSTART typescript-activity-timeouts-heartbeat-details
export async function myActivity(): Promise<void> {
  // Resume work from latest heartbeat, if there's one, or start from 1 otherwise
  const startingPoint = activityInfo().heartbeatDetails?.progress ?? 1;

  for (let progress = startingPoint; progress <= 1000; ++progress) {
    // Do something that takes time
    await sleep('1s');

    heartbeat({ progress });
  }
}
// @@@SNIPEND

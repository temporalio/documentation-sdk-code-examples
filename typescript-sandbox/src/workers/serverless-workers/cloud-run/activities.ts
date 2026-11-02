// @@@SNIPSTART typescript-cloud-run-heartbeat
import { activityInfo, heartbeat } from '@temporalio/activity';

export async function myActivity(items: string[]): Promise<string> {
  // Resume from the last item recorded by a Heartbeat, if this is a retry
  const start: number = activityInfo().heartbeatDetails ?? 0;
  for (let i = start; i < items.length; i++) {
    heartbeat(i);
    // ... process items[i]
  }
  return 'done';
}
// @@@SNIPEND

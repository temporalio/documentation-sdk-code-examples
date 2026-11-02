import { Client } from '@temporalio/client';
import { runWithClient } from './connect';

// @@@SNIPSTART typescript-schedules-pause
export async function pauseSchedule(client: Client): Promise<void> {
  const handle = client.schedule.getHandle('sample-schedule');
  await handle.pause();

  console.log(`Schedule is now paused.`);
}
// @@@SNIPEND

if (require.main === module) {
  void runWithClient(pauseSchedule);
}

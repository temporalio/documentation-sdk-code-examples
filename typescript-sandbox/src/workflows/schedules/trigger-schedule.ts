import { Client } from '@temporalio/client';
import { runWithClient } from './connect';

// @@@SNIPSTART typescript-schedules-trigger
export async function triggerSchedule(client: Client): Promise<void> {
  const handle = client.schedule.getHandle('sample-schedule');

  await handle.trigger();

  console.log(`Schedule is now triggered.`);
}
// @@@SNIPEND

if (require.main === module) {
  void runWithClient(triggerSchedule);
}

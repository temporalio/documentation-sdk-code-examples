import { Client } from '@temporalio/client';
import { runWithClient } from './connect';

// @@@SNIPSTART typescript-schedules-delete
export async function deleteSchedule(client: Client): Promise<void> {
  const handle = client.schedule.getHandle('sample-schedule');
  await handle.delete();

  console.log(`Schedule is now deleted.`);
}
// @@@SNIPEND

if (require.main === module) {
  void runWithClient(deleteSchedule);
}

import { Backfill, Client, ScheduleOverlapPolicy } from '@temporalio/client';
import { runWithClient } from './connect';

// @@@SNIPSTART typescript-schedules-backfill
function subtractMinutes(minutes: number): Date {
  const now = new Date();
  return new Date(now.getTime() - minutes * 60 * 1000);
}

export async function backfillSchedule(client: Client): Promise<void> {
  const backfillOptions: Backfill = {
    start: subtractMinutes(10),
    end: subtractMinutes(9),
    overlap: ScheduleOverlapPolicy.ALLOW_ALL,
  };

  const handle = client.schedule.getHandle('sample-schedule');
  await handle.backfill(backfillOptions);

  console.log(`Schedule is now backfilled.`);
}
// @@@SNIPEND

if (require.main === module) {
  void runWithClient(backfillSchedule);
}

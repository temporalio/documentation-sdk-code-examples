import { Client, ScheduleSummary } from '@temporalio/client';
import { runWithClient } from './connect';

// @@@SNIPSTART typescript-schedules-list
export async function listSchedules(client: Client): Promise<ScheduleSummary[]> {
  const schedules = [];

  const scheduleList = client.schedule.list();

  for await (const schedule of scheduleList) {
    schedules.push(schedule);
  }

  console.log(`Schedules are now listed: ${JSON.stringify(schedules)}`);
  return schedules;
}
// @@@SNIPEND

if (require.main === module) {
  void runWithClient(listSchedules);
}

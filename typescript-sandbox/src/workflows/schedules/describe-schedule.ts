import { Client, ScheduleDescription } from '@temporalio/client';
import { runWithClient } from './connect';

// @@@SNIPSTART typescript-schedules-describe
export async function describeSchedule(client: Client): Promise<ScheduleDescription> {
  const handle = client.schedule.getHandle('sample-schedule');

  const result = await handle.describe();

  console.log(`Schedule description: ${JSON.stringify(result)}`);
  return result;
}
// @@@SNIPEND

if (require.main === module) {
  void runWithClient(describeSchedule);
}

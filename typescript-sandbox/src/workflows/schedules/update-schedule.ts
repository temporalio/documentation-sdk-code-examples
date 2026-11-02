import { Client, ScheduleDescription, ScheduleUpdateOptions } from '@temporalio/client';
import { runWithClient } from './connect';

// @@@SNIPSTART typescript-schedules-update
const updateScheduleArgs = (input: ScheduleDescription): ScheduleUpdateOptions => {
  return { ...input, action: { ...input.action, args: ['my updated schedule arg'] } };
};

export async function updateSchedule(client: Client): Promise<void> {
  const handle = client.schedule.getHandle('sample-schedule');

  await handle.update(updateScheduleArgs);

  console.log(`Schedule is now updated.`);
}
// @@@SNIPEND

if (require.main === module) {
  void runWithClient(updateSchedule);
}

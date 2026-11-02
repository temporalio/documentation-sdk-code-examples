import { Client, ScheduleHandle, ScheduleOverlapPolicy } from '@temporalio/client';
import { runWithClient } from './connect';
import { reminder } from './workflows';

// @@@SNIPSTART typescript-schedules-create
export async function createSchedule(client: Client): Promise<ScheduleHandle> {
  // https://typescript.temporal.io/api/classes/client.ScheduleClient#create
  const schedule = await client.schedule.create({
    action: {
      type: 'startWorkflow',
      workflowType: reminder,
      args: ['♻️ Dear future self, please take out the recycling tonight. Sincerely, past you ❤️'],
      taskQueue: 'schedules',
    },
    scheduleId: 'sample-schedule',
    policies: {
      catchupWindow: '1 day',
      overlap: ScheduleOverlapPolicy.ALLOW_ALL,
    },
    spec: {
      intervals: [{ every: '10s' }],
      // or periodic calendar times:
      // calendars: [
      //   {
      //     comment: 'every wednesday at 8:30pm',
      //     dayOfWeek: 'WEDNESDAY',
      //     hour: 20,
      //     minute: 30,
      //   },
      // ],
      // or a single datetime:
      // calendars: [
      //   {
      //     comment: '1/1/23 at 9am',
      //     year: 2023,
      //     month: 1,
      //     dayOfMonth: 1,
      //     hour: 9,
      //   },
      // ],
    },
  });

  console.log(`Started schedule '${schedule.scheduleId}'.`);
  return schedule;
}
// @@@SNIPEND

if (require.main === module) {
  void runWithClient(createSchedule);
}

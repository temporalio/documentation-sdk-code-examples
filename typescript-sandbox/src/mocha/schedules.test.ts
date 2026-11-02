import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import { Client, ScheduleNotFoundError } from '@temporalio/client';
import { createSchedule } from '../workflows/schedules/create-schedule';
import { backfillSchedule } from '../workflows/schedules/backfill-schedule';
import { describeSchedule } from '../workflows/schedules/describe-schedule';
import { listSchedules } from '../workflows/schedules/list-schedule';
import { pauseSchedule } from '../workflows/schedules/pause-schedule';
import { triggerSchedule } from '../workflows/schedules/trigger-schedule';
import { updateSchedule } from '../workflows/schedules/update-schedule';
import { deleteSchedule } from '../workflows/schedules/delete-schedule';
import { startCronWorkflow } from '../workflows/schedules/cron';
import { startDelayedWorkflow } from '../workflows/schedules/start-delay';
import assert from 'assert';

async function eventually<T>(fn: () => Promise<T>, check: (value: T) => boolean): Promise<T> {
  for (let i = 0; ; i++) {
    const value = await fn();
    if (check(value) || i >= 50) return value;
    await new Promise((resolve) => setTimeout(resolve, 200));
  }
}

describe('Schedules', function () {
  this.timeout(60_000);
  let testEnv: TestWorkflowEnvironment;
  let client: Client;
  let worker: Worker;
  let runPromise: Promise<void>;

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
    client = testEnv.client;
    worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue: 'schedules',
      workflowsPath: require.resolve('../workflows/schedules/workflows'),
    });
    runPromise = worker.run();
  });

  after(async () => {
    worker?.shutdown();
    await runPromise;
    await testEnv?.teardown();
  });

  // These run in order against the same 'sample-schedule'
  it('creates a Schedule', async () => {
    const handle = await createSchedule(client);
    assert.equal(handle.scheduleId, 'sample-schedule');
  });

  it('describes the Schedule', async () => {
    const description = await describeSchedule(client);
    assert.equal(description.action.workflowType, 'reminder');
  });

  it('lists the Schedule', async () => {
    const schedules = await eventually(
      () => listSchedules(client),
      (list) => list.some((s) => s.scheduleId === 'sample-schedule'),
    );
    assert.ok(schedules.some((s) => s.scheduleId === 'sample-schedule'));
  });

  it('pauses the Schedule', async () => {
    await pauseSchedule(client);
    assert.equal((await describeSchedule(client)).state.paused, true);
  });

  it('triggers the Schedule', async () => {
    const before = (await describeSchedule(client)).info.recentActions.length;
    await triggerSchedule(client);
    const description = await eventually(
      () => describeSchedule(client),
      (d) => d.info.recentActions.length > before,
    );
    assert.ok(description.info.recentActions.length > before);
  });

  it('updates the Schedule', async () => {
    await updateSchedule(client);
    const description = await describeSchedule(client);
    assert.deepEqual(description.action.args, ['my updated schedule arg']);
    assert.equal(description.action.workflowType, 'reminder');
  });

  it('backfills the Schedule', async () => {
    await backfillSchedule(client);
  });

  it('deletes the Schedule', async () => {
    await deleteSchedule(client);
    await assert.rejects(describeSchedule(client), ScheduleNotFoundError);
  });

  it('starts a Workflow with a Cron Schedule', async () => {
    const handle = await startCronWorkflow(client, 'schedules', 'cron-workflow');
    const history = await handle.fetchHistory();
    assert.equal(history.events?.[0].workflowExecutionStartedEventAttributes?.cronSchedule, '* * * * *');
    await handle.terminate();
  });

  it('starts a Workflow with a Start Delay', async () => {
    const handle = await startDelayedWorkflow(client, 'schedules', 'delayed-workflow');
    const history = await handle.fetchHistory();
    const delay = history.events?.[0].workflowExecutionStartedEventAttributes?.firstWorkflowTaskBackoff;
    assert.equal(Number(delay?.seconds), 2 * 60 * 60);
    await handle.terminate();
  });
});

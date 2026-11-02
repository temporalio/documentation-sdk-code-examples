import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import * as activities from '../workflows/timers/activities';
import {
  completeUserInteraction,
  countdownWorkflow,
  processOrderWorkflow,
  setDeadlineSignal,
  sleepThenAntipatternWorkflow,
  timeLeftQuery,
  yourWorkflow,
} from '../workflows/timers/workflows';
import assert from 'assert';

const taskQueue = 'timers';

async function withWorker<T>(testEnv: TestWorkflowEnvironment, fn: () => Promise<T>): Promise<T> {
  const worker = await Worker.create({
    connection: testEnv.nativeConnection,
    taskQueue,
    workflowsPath: require.resolve('../workflows/timers/workflows'),
    activities,
  });
  return worker.runUntil(fn());
}

describe('Timers', function () {
  this.timeout(60_000);
  let testEnv: TestWorkflowEnvironment;

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
  });

  after(async () => {
    await testEnv?.teardown();
  });

  it('sends a notification email only when processing outlasts the Timer', async () => {
    await withWorker(testEnv, async () => {
      activities.calls.length = 0;
      await testEnv.client.workflow.execute(processOrderWorkflow, {
        taskQueue,
        workflowId: 'racing-timers-fast',
        args: [{ orderProcessingMS: 100, sendDelayedEmailTimeoutMS: 2000 }],
      });
      assert.deepEqual(activities.calls, ['processOrder']);

      activities.calls.length = 0;
      await testEnv.client.workflow.execute(processOrderWorkflow, {
        taskQueue,
        workflowId: 'racing-timers-slow',
        args: [{ orderProcessingMS: 2000, sendDelayedEmailTimeoutMS: 200 }],
      });
      assert.deepEqual(activities.calls, ['sendNotificationEmail', 'processOrder']);
    });
  });

  it('skips the reminder when the Signal arrives first', async () => {
    await withWorker(testEnv, async () => {
      activities.calls.length = 0;
      const handle = await testEnv.client.workflow.start(yourWorkflow, {
        taskQueue,
        workflowId: 'racing-signals',
        args: ['user-1'],
      });
      await handle.signal(completeUserInteraction);
      await handle.result();
      assert.deepEqual(activities.calls, []);
    });
  });

  it('keeps running the chained .then after the race resolves', async () => {
    const status = await withWorker(testEnv, () =>
      testEnv.client.workflow.execute(sleepThenAntipatternWorkflow, { taskQueue, workflowId: 'sleep-then' }),
    );
    // somethingElse won the race, but the 5-second Timer fired while complete() ran
    assert.equal(status, 'timed_out');
  });

  it('resolves an updatable Timer early when the deadline is moved', async () => {
    await withWorker(testEnv, async () => {
      const handle = await testEnv.client.workflow.start(countdownWorkflow, { taskQueue, workflowId: 'countdown' });
      assert.ok((await handle.query(timeLeftQuery)) > 60 * 60 * 1000);
      await handle.signal(setDeadlineSignal, Date.now() + 1000);
      await handle.result();
    });
  });
});

describe('Timers with time skipping', function () {
  this.timeout(120_000);
  let testEnv: TestWorkflowEnvironment;

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createTimeSkipping();
  });

  after(async () => {
    await testEnv?.teardown();
  });

  it('sends a reminder when no Signal arrives within 30 days', async () => {
    await withWorker(testEnv, async () => {
      activities.calls.length = 0;
      await testEnv.client.workflow.execute(yourWorkflow, {
        taskQueue,
        workflowId: 'racing-signals-reminder',
        args: ['user-1'],
      });
      assert.deepEqual(activities.calls, ['sendReminderEmail:user-1']);
    });
  });
});

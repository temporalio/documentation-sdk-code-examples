import { MockActivityEnvironment, TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import { WorkflowHandle } from '@temporalio/client';
import { CancelledFailure } from '@temporalio/common';
import * as activities from '../activities/timeouts/activities';
import { myActivity as heartbeatActivity } from '../activities/timeouts/heartbeat';
import { myActivity as checkpointActivity } from '../activities/timeouts/heartbeat-checkpoint';
import { heartbeatTimeoutWorkflow, retryWorkflow, timeoutsWorkflow } from '../activities/timeouts/workflows';
import assert from 'assert';

const seconds = (d?: { seconds?: unknown; nanos?: number | null } | null) =>
  Number(d?.seconds ?? 0) + (d?.nanos ?? 0) / 1e9;

async function scheduledActivity(handle: WorkflowHandle) {
  for (let i = 0; i < 50; i++) {
    const history = await handle.fetchHistory();
    const event = history.events?.find((e) => e.activityTaskScheduledEventAttributes);
    if (event) return event.activityTaskScheduledEventAttributes!;
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
  throw new Error('Activity was not scheduled');
}

async function pendingActivity<T>(handle: WorkflowHandle, pick: (p: any) => T | undefined): Promise<T> {
  for (let i = 0; i < 100; i++) {
    const pending = (await handle.describe()).raw.pendingActivities?.[0];
    const value = pending && pick(pending);
    if (value !== undefined && value !== null) return value;
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
  throw new Error('Pending Activity never reached the expected state');
}

describe('Activity Timeouts', function () {
  this.timeout(60_000);
  let testEnv: TestWorkflowEnvironment;
  let worker: Worker;
  let runPromise: Promise<void>;
  const taskQueue = 'activity-timeouts';

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
    worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      workflowsPath: require.resolve('../activities/timeouts/workflows'),
      activities,
    });
    runPromise = worker.run();
  });

  after(async () => {
    worker?.shutdown();
    await runPromise;
    await testEnv?.teardown();
  });

  it('sets the Schedule-To-Close Timeout', async () => {
    const handle = await testEnv.client.workflow.start(timeoutsWorkflow, { taskQueue, workflowId: 'timeouts' });
    const scheduled = await scheduledActivity(handle);
    assert.equal(seconds(scheduled.scheduleToCloseTimeout), 5 * 60);
    await handle.terminate();
  });

  it('sets the Retry Policy, and nextRetryDelay overrides the next interval', async () => {
    const handle = await testEnv.client.workflow.start(retryWorkflow, { taskQueue, workflowId: 'retry' });
    const scheduled = await scheduledActivity(handle);
    assert.equal(seconds(scheduled.retryPolicy?.initialInterval), 10);
    assert.equal(scheduled.retryPolicy?.maximumAttempts, 5);

    // After the first failure, the next attempt waits 15 seconds instead of the Retry Policy's 10
    const pending = await pendingActivity(handle, (p) => (p.nextAttemptScheduleTime ? p : undefined));
    const delay = seconds(pending.nextAttemptScheduleTime) - seconds(pending.lastAttemptCompleteTime);
    assert.ok(Math.abs(delay - 15) < 1, `next retry in ${delay}s`);
    await handle.terminate();
  });

  it('sets the Heartbeat Timeout and records Heartbeat details', async () => {
    const handle = await testEnv.client.workflow.start(heartbeatTimeoutWorkflow, {
      taskQueue,
      workflowId: 'heartbeat-timeout',
    });
    const scheduled = await scheduledActivity(handle);
    assert.equal(seconds(scheduled.heartbeatTimeout), 30);
    const details = await pendingActivity(handle, (p) => p.heartbeatDetails?.payloads?.length);
    assert.ok(details > 0);
    await handle.terminate();
  });

  it('observes cancellation while it heartbeats', async () => {
    const env = new MockActivityEnvironment();
    let heartbeats = 0;
    env.on('heartbeat', () => {
      if (++heartbeats === 2) env.cancel();
    });
    await assert.rejects(env.run(heartbeatActivity), CancelledFailure);
    assert.equal(heartbeats, 2);
  });

  it('resumes from the details of the last Heartbeat', async () => {
    const env = new MockActivityEnvironment({ heartbeatDetails: { progress: 999 } });
    const details: unknown[] = [];
    env.on('heartbeat', (d: unknown) => details.push(d));
    await env.run(checkpointActivity);
    assert.deepEqual(details, [{ progress: 999 }, { progress: 1000 }]);
  });
});

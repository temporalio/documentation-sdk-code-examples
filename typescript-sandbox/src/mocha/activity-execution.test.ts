import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import * as activities from '../activities/execution/activities';
import { example } from '../activities/execution/workflows';
import assert from 'assert';

describe('Activity Execution', function () {
  this.timeout(60_000);
  let testEnv: TestWorkflowEnvironment;

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
  });

  after(async () => {
    await testEnv?.teardown();
  });

  it('calls an Activity through the Activity Handle and records its Events', async () => {
    const taskQueue = 'activity-execution';
    const worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      workflowsPath: require.resolve('../activities/execution/workflows'),
      activities,
    });
    await worker.runUntil(async () => {
      const handle = await testEnv.client.workflow.start(example, {
        taskQueue,
        workflowId: 'activity-execution',
        args: ['Temporal'],
      });
      assert.equal(await handle.result(), 'Hello, Temporal!');

      // The three Activity Task Events the page describes
      const history = await handle.fetchHistory();
      const types = history.events?.map((e) => Object.keys(e).find((k) => k.endsWith('EventAttributes')));
      for (const attrs of [
        'activityTaskScheduledEventAttributes',
        'activityTaskStartedEventAttributes',
        'activityTaskCompletedEventAttributes',
      ]) {
        assert.ok(types?.includes(attrs), attrs);
      }
    });
  });
});

import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import * as asyncActivities from '../activities/async-completion/activities';
import { asyncCompletionWorkflow } from '../activities/async-completion/workflows';
import * as localActivities from '../activities/local-activities/activities';
import { yourWorkflow } from '../activities/local-activities/workflows';
import assert from 'assert';

describe('Asynchronous Activity Completion and Local Activities', function () {
  this.timeout(60_000);
  let testEnv: TestWorkflowEnvironment;

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
  });

  after(async () => {
    await testEnv?.teardown();
  });

  it('completes an Activity from outside the Activity Function', async () => {
    // doSomeWork() connects with loadClientConnectConfig(), which reads the address from the environment
    process.env.TEMPORAL_ADDRESS = testEnv.address;
    const taskQueue = 'async-completion';
    const worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      workflowsPath: require.resolve('../activities/async-completion/workflows'),
      activities: asyncActivities,
    });
    try {
      const result = await worker.runUntil(
        testEnv.client.workflow.execute(asyncCompletionWorkflow, { taskQueue, workflowId: 'async-completion' }),
      );
      assert.equal(result, "Job's done!");
    } finally {
      delete process.env.TEMPORAL_ADDRESS;
    }
  });

  it('calls a Local Activity', async () => {
    process.env.SOME_SETTING = 'enabled';
    const taskQueue = 'local-activities';
    const worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      workflowsPath: require.resolve('../activities/local-activities/workflows'),
      activities: localActivities,
    });
    const handle = await testEnv.client.workflow.start(yourWorkflow, { taskQueue, workflowId: 'local-activity' });
    const result = await worker.runUntil(handle.result());
    delete process.env.SOME_SETTING;
    assert.equal(result, 'enabled');

    // A Local Activity is recorded as a Marker, not as Activity Task Events
    const history = await handle.fetchHistory();
    assert.ok(history.events?.some((e) => e.markerRecordedEventAttributes));
    assert.ok(!history.events?.some((e) => e.activityTaskScheduledEventAttributes));
  });
});

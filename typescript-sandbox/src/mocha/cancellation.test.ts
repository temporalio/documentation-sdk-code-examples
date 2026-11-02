import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import { WorkflowFailedError } from '@temporalio/client';
import { CancelledFailure } from '@temporalio/common';
import * as activities from '../workflows/cancellation/activities';
import { orderWorkflow } from '../workflows/cancellation/workflows';
import assert from 'assert';

describe('Activity cancellation type', function () {
  this.timeout(60_000);
  let testEnv: TestWorkflowEnvironment;

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
  });

  after(async () => {
    await testEnv?.teardown();
  });

  it('waits for the Activity to finish cancelling before the Workflow is cancelled', async () => {
    const taskQueue = 'cancellation';
    const worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      workflowsPath: require.resolve('../workflows/cancellation/workflows'),
      activities,
      // Send Heartbeats (and receive the cancellation) sooner than 80% of the 30-second Heartbeat Timeout
      maxHeartbeatThrottleInterval: '200 milliseconds',
    });

    await worker.runUntil(async () => {
      const handle = await testEnv.client.workflow.start(orderWorkflow, {
        taskQueue,
        workflowId: 'cancellation-order',
        args: ['order-1'],
      });
      // Let the Activity start heartbeating before cancelling
      await new Promise((resolve) => setTimeout(resolve, 1000));
      await handle.cancel();

      await assert.rejects(
        handle.result(),
        (err) => err instanceof WorkflowFailedError && err.cause instanceof CancelledFailure,
      );
      // With WAIT_CANCELLATION_COMPLETED, the Workflow only closes after the Activity ran its cleanup
      assert.equal(activities.cleanup.ran, true);
    });
  });
});

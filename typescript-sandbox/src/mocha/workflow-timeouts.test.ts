import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import { WorkflowFailedError, WorkflowHandle } from '@temporalio/client';
import { ActivityFailure, ApplicationFailure, Workflow } from '@temporalio/common';
import * as activities from '../workflows/timeouts/activities';
import { addAddressWorkflow, chargeWorkflow } from '../workflows/timeouts/workflows';
import { startWithRetryPolicy, startWithTimeouts } from '../workflows/timeouts/client';
import assert from 'assert';

// The Activity's attempt number when it last started, from the Event History
async function lastActivityAttempt(handle: WorkflowHandle<Workflow>): Promise<number | null | undefined> {
  const history = await handle.fetchHistory();
  const started = history.events?.filter((e) => e.activityTaskStartedEventAttributes).pop();
  return started?.activityTaskStartedEventAttributes?.attempt;
}

describe('Workflow timeouts and failures', function () {
  this.timeout(60_000);
  let testEnv: TestWorkflowEnvironment;
  let worker: Worker;
  let runPromise: Promise<void>;
  const taskQueue = 'workflow-timeouts';

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
    worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      workflowsPath: require.resolve('../workflows/timeouts/workflows'),
      activities,
    });
    runPromise = worker.run();
  });

  after(async () => {
    worker?.shutdown();
    await runPromise;
    await testEnv?.teardown();
  });

  async function runCharge(method: 'customError' | 'applicationFailure' | 'nonRetryable') {
    const handle = await testEnv.client.workflow.start(chargeWorkflow, {
      taskQueue,
      workflowId: `charge-${method}`,
      args: [method, -5],
    });
    const err = await handle.result().catch((e) => e);
    assert.ok(err instanceof WorkflowFailedError && err.cause instanceof ActivityFailure);
    assert.ok(err.cause.cause instanceof ApplicationFailure);
    return { failure: err.cause.cause, attempt: await lastActivityAttempt(handle) };
  }

  it('converts a custom error to a retryable ApplicationFailure', async () => {
    const { failure, attempt } = await runCharge('customError');
    assert.equal(failure.type, 'InvalidChargeError');
    assert.equal(failure.nonRetryable, false);
    assert.equal(attempt, 3);
  });

  it('throws an ApplicationFailure with the same type', async () => {
    const { failure, attempt } = await runCharge('applicationFailure');
    assert.equal(failure.type, 'InvalidChargeError');
    assert.equal(attempt, 3);
  });

  it('does not retry a non-retryable ApplicationFailure', async () => {
    const { failure, attempt } = await runCharge('nonRetryable');
    assert.equal(failure.nonRetryable, true);
    assert.equal(attempt, 1);
  });

  it('fails the Workflow by re-throwing the ActivityFailure', async () => {
    await assert.rejects(
      testEnv.client.workflow.execute(addAddressWorkflow, { taskQueue, workflowId: 'add-address' }),
      (err) => err instanceof WorkflowFailedError && err.cause instanceof ActivityFailure,
    );
  });

  it('starts a Workflow with a Workflow Execution Timeout', async () => {
    const handle = await startWithTimeouts(testEnv.client, taskQueue, 'with-timeouts');
    const description = await handle.describe();
    assert.equal(Number(description.raw.executionConfig?.workflowExecutionTimeout?.seconds), 24 * 60 * 60);
    await handle.result();
  });

  it('starts a Workflow with a Retry Policy', async () => {
    const handle = await startWithRetryPolicy(testEnv.client, taskQueue, 'with-retry');
    assert.equal(await handle.result(), 'done');
  });
});

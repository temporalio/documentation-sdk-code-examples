import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import { WorkflowFailedError } from '@temporalio/client';
import { ActivityFailure, ApplicationFailure, ApplicationFailureCategory } from '@temporalio/common';
import * as activities from '../activities/benign-exceptions/activities';
import { benignExceptionWorkflow } from '../activities/benign-exceptions/workflows';
import assert from 'assert';

describe('Benign exceptions', function () {
  this.timeout(60_000);
  let testEnv: TestWorkflowEnvironment;

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
  });

  after(async () => {
    await testEnv?.teardown();
  });

  it('fails the Activity with a benign ApplicationFailure', async () => {
    const taskQueue = 'benign-exceptions';
    const worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      workflowsPath: require.resolve('../activities/benign-exceptions/workflows'),
      activities,
    });
    const err = await worker
      .runUntil(testEnv.client.workflow.execute(benignExceptionWorkflow, { taskQueue, workflowId: 'benign' }))
      .catch((e) => e);
    assert.ok(err instanceof WorkflowFailedError && err.cause instanceof ActivityFailure);
    const failure = err.cause.cause;
    assert.ok(failure instanceof ApplicationFailure);
    assert.equal(failure.message, 'External service is not ready yet');
    assert.equal(failure.category, ApplicationFailureCategory.BENIGN);
  });
});

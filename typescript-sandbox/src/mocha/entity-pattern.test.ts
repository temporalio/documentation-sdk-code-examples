import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import { WorkflowFailedError } from '@temporalio/client';
import { CancelledFailure } from '@temporalio/common';
import * as activities from '../best-practices/entity-pattern/activities';
import { entityWorkflow, updateSignal } from '../best-practices/entity-pattern/workflows';
import assert from 'assert';

describe('Entity pattern', function () {
  this.timeout(60_000);
  let testEnv: TestWorkflowEnvironment;
  let worker: Worker;
  let runPromise: Promise<void>;
  const taskQueue = 'entity-pattern';

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
    worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      workflowsPath: require.resolve('../best-practices/entity-pattern/workflows'),
      activities,
    });
    runPromise = worker.run();
  });

  after(async () => {
    worker?.shutdown();
    await runPromise;
    await testEnv?.teardown();
  });

  async function waitFor(check: () => boolean | Promise<boolean>) {
    for (let i = 0; i < 100 && !(await check()); i++) {
      await new Promise((resolve) => setTimeout(resolve, 100));
    }
  }

  it('processes Signals, then continues as new without running setup again', async () => {
    activities.calls.length = 0;
    const handle = await testEnv.client.workflow.start(entityWorkflow, {
      taskQueue,
      workflowId: 'entity-1',
      args: [{ entityId: 'entity-1' }],
    });
    await handle.signal(updateSignal, { value: 1 });
    await waitFor(() => activities.calls.includes('update 1'));

    // MAX_ITERATIONS is 1, so the first run continued as new after handling the Signal
    const firstRun = testEnv.client.workflow.getHandle('entity-1', handle.firstExecutionRunId);
    await waitFor(async () => (await firstRun.describe()).status.name === 'CONTINUED_AS_NEW');
    assert.equal((await firstRun.describe()).status.name, 'CONTINUED_AS_NEW');

    // The next run receives isNew = false, so it skips setup
    await handle.signal(updateSignal, { value: 2 });
    await waitFor(() => activities.calls.includes('update 2'));
    assert.deepEqual(activities.calls, ['setup entity-1', 'update 1', 'update 2']);

    await handle.terminate();
  });

  it('runs cleanup in a non-cancellable scope when the entity is canceled', async () => {
    activities.calls.length = 0;
    const handle = await testEnv.client.workflow.start(entityWorkflow, {
      taskQueue,
      workflowId: 'entity-2',
      args: [{ entityId: 'entity-2' }],
    });
    await waitFor(() => activities.calls.includes('setup entity-2'));
    await handle.cancel();
    await assert.rejects(
      handle.result(),
      (err) => err instanceof WorkflowFailedError && err.cause instanceof CancelledFailure,
    );
    assert.deepEqual(activities.calls, ['setup entity-2', 'cleanup']);
  });
});

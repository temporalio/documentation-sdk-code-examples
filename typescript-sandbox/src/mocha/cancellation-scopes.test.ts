import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import { WorkflowClient, WorkflowFailedError } from '@temporalio/client';
import { ActivityFailure, CancelledFailure } from '@temporalio/common';
import * as activities from '../workflows/cancellation-scopes/activities';
import {
  cancelTimer,
  cancelTimerAltImpl,
  cancellationScopesWithCallbacks,
  handleExternalWorkflowCancellationWhileActivityRunning,
  multipleActivitiesSingleTimeout,
  nestedCancellation,
  nonCancellable,
  resumeAfterCancellation,
  sharedScopes,
  shieldAwaitedInRootScope,
} from '../workflows/cancellation-scopes/workflows';
import assert from 'assert';

const isCancelledWorkflow = (err: unknown) => err instanceof WorkflowFailedError && err.cause instanceof CancelledFailure;

describe('Cancellation scopes', function () {
  this.timeout(60_000);
  let testEnv: TestWorkflowEnvironment;
  let worker: Worker;
  let workflowClient: WorkflowClient;
  const taskQueue = 'cancellation-scopes';
  let runPromise: Promise<void>;
  let id = 0;
  const workflowId = () => `cancellation-scopes-${++id}`;

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
    workflowClient = testEnv.client.workflow;
    worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      workflowsPath: require.resolve('../workflows/cancellation-scopes/workflows'),
      activities,
      // Send Heartbeats often so cancellation reaches the Activities quickly
      defaultHeartbeatThrottleInterval: '100 milliseconds',
      maxHeartbeatThrottleInterval: '100 milliseconds',
    });
    runPromise = worker.run();
  });

  after(async () => {
    worker?.shutdown();
    await runPromise;
    await testEnv?.teardown();
  });

  // Starts a Workflow, waits for its Activity to be running, and cancels it
  async function startAndCancel<T>(handlePromise: Promise<{ cancel(): Promise<unknown>; result(): Promise<T> }>) {
    const handle = await handlePromise;
    await new Promise((resolve) => setTimeout(resolve, 500));
    await handle.cancel();
    return handle.result();
  }

  it('cancels a Timer from inside the Workflow', async () => {
    await workflowClient.execute(cancelTimer, { taskQueue, workflowId: workflowId() });
    await workflowClient.execute(cancelTimerAltImpl, { taskQueue, workflowId: workflowId() });
  });

  it('runs cleanup in a nonCancellable scope after external cancellation', async () => {
    const url = 'http://slow.example.com/post';
    await assert.rejects(
      startAndCancel(
        workflowClient.start(handleExternalWorkflowCancellationWhileActivityRunning, {
          taskQueue,
          workflowId: workflowId(),
          args: [url, { some: 'data' }],
        }),
      ),
      isCancelledWorkflow,
    );
    assert.ok(activities.cleanupCalls.includes(url));
  });

  it('shields an Activity from cancellation with nonCancellable', async () => {
    const url = 'http://slow.example.com/non-cancellable';
    const result = await startAndCancel(
      workflowClient.start(nonCancellable, { taskQueue, workflowId: workflowId(), args: [url] }),
    );
    assert.deepEqual(result, { url });
  });

  it('cancels Activities when withTimeout elapses', async () => {
    const urls = ['http://a.example.com', 'http://b.example.com'];
    const result = await workflowClient.execute(multipleActivitiesSingleTimeout, {
      taskQueue,
      workflowId: workflowId(),
      args: [urls, 5000],
    });
    assert.deepEqual(result, urls.map((url) => ({ url })));

    await assert.rejects(
      workflowClient.execute(multipleActivitiesSingleTimeout, {
        taskQueue,
        workflowId: workflowId(),
        args: [['http://slow.example.com'], 500],
      }),
      (err) => err instanceof WorkflowFailedError,
    );
  });

  it('waits for a nonCancellable Activity after cancelRequested', async () => {
    const url = 'http://slow.example.com/resume';
    const result = await startAndCancel(
      workflowClient.start(resumeAfterCancellation, { taskQueue, workflowId: workflowId(), args: [url] }),
    );
    assert.deepEqual(result, { url });
  });

  it('completes a callback that listens for cancelRequested', async () => {
    await workflowClient.execute(cancellationScopesWithCallbacks, { taskQueue, workflowId: workflowId() });
  });

  it('runs cleanup when a nested withTimeout scope times out', async () => {
    await workflowClient.execute(nestedCancellation, {
      taskQueue,
      workflowId: workflowId(),
      args: ['http://fast.example.com'],
    });

    const url = 'http://slow.example.com/nested';
    await assert.rejects(
      workflowClient.execute(nestedCancellation, { taskQueue, workflowId: workflowId(), args: [url] }),
      (err) =>
        err instanceof WorkflowFailedError &&
        err.cause instanceof ActivityFailure &&
        err.cause.cause instanceof CancelledFailure,
    );
    assert.ok(activities.cleanupCalls.includes(url));
  });

  it('shares Activity promises between scopes', async () => {
    const first = await workflowClient.execute(sharedScopes, { taskQueue, workflowId: workflowId() });
    assert.ok(['http://url1.ninja', 'http://url2.ninja'].includes(first.url));

    const shielded = await workflowClient.execute(shieldAwaitedInRootScope, { taskQueue, workflowId: workflowId() });
    assert.deepEqual(shielded, { url: 'http://example.com' });
  });
});

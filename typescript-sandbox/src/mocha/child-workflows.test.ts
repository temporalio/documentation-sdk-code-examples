import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import { WorkflowFailedError } from '@temporalio/client';
import { CancelledFailure } from '@temporalio/common';
import { parentWorkflow as startChildParent } from '../workflows/child-workflows/start-child';
import { parentWorkflow as executeChildParent } from '../workflows/child-workflows/execute-child';
import { cancelWorkflow, longRunningWorkflow } from '../workflows/child-workflows/external-handle';
import assert from 'assert';

describe('Child Workflows', function () {
  this.timeout(60_000);
  let testEnv: TestWorkflowEnvironment;

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
  });

  after(async () => {
    await testEnv?.teardown();
  });

  // Two files define a `parentWorkflow`, so each file gets its own Worker.
  async function runWith<T>(workflowFile: string, fn: (taskQueue: string) => Promise<T>): Promise<T> {
    const taskQueue = `child-workflows-${workflowFile}`;
    const worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      workflowsPath: require.resolve(`../workflows/child-workflows/${workflowFile}`),
    });
    return worker.runUntil(fn(taskQueue));
  }

  it('starts a Child Workflow, signals it, and gets its result', async () => {
    const result = await runWith('start-child', (taskQueue) =>
      testEnv.client.workflow.execute(startChildParent, {
        args: ['Alice'],
        workflowId: 'child-workflows-start-child',
        taskQueue,
      }),
    );
    assert.equal(result, 'I am a child named Alice');
  });

  it('executes Child Workflows and waits for them to complete', async () => {
    const result = await runWith('execute-child', (taskQueue) =>
      testEnv.client.workflow.execute(executeChildParent, {
        args: ['Alice', 'Bob'],
        workflowId: 'child-workflows-execute-child',
        taskQueue,
      }),
    );
    assert.equal(result, 'I am a child named Alice\nI am a child named Bob');
  });

  it('cancels another Workflow through an external handle', async () => {
    await runWith('external-handle', async (taskQueue) => {
      const target = await testEnv.client.workflow.start(longRunningWorkflow, {
        workflowId: 'child-workflows-cancel-target',
        taskQueue,
      });
      await testEnv.client.workflow.execute(cancelWorkflow, {
        args: [target.workflowId],
        workflowId: 'child-workflows-external-handle',
        taskQueue,
      });
      await assert.rejects(
        target.result(),
        (err) => err instanceof WorkflowFailedError && err.cause instanceof CancelledFailure,
      );
    });
  });
});

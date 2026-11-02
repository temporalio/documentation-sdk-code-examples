import { TestWorkflowEnvironment, workflowInterceptorModules } from '@temporalio/testing';
import { Worker } from '@temporalio/worker';
import { WorkflowFailedError } from '@temporalio/client';
import { nanoid } from 'nanoid';
import assert from 'assert';
import { functionToTest } from '../workflows/file-with-workflow-function-to-test';
import { functionToTest as functionWithChild } from '../workflows/file-with-child-workflow';
import { functionToTest as functionWithAssert } from '../workflows/file-with-assert';

describe('Test functions in Workflow context', () => {
  let testEnv: TestWorkflowEnvironment;
  const taskQueue = 'test';
  const workflowOptions = () => ({ workflowId: nanoid(), taskQueue });

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createTimeSkipping();
  });

  after(async () => {
    await testEnv?.teardown();
  });

  it('runs a function from the Workflow bundle as a Workflow', async () => {
    // @@@SNIPSTART typescript-testing-run-function-to-test
    const worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      workflowsPath: require.resolve('../workflows/file-with-workflow-function-to-test'),
    });

    const result = await worker.runUntil(testEnv.client.workflow.execute(functionToTest, workflowOptions()));

    assert.equal(result, 42);
    // @@@SNIPEND
  });

  it('runs a function that starts a Child Workflow exported from the same file', async () => {
    const worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      workflowsPath: require.resolve('../workflows/file-with-child-workflow'),
    });
    const result = await worker.runUntil(testEnv.client.workflow.execute(functionWithChild, workflowOptions()));
    assert.equal(result, 42);
  });

  it('fails the Workflow Execution on a failed assert', async () => {
    // @@@SNIPSTART typescript-testing-workflow-interceptor-modules
    const worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      interceptors: {
        workflowModules: workflowInterceptorModules,
      },
      workflowsPath: require.resolve('../workflows/file-with-assert'),
    });

    await assert.rejects(
      worker.runUntil(testEnv.client.workflow.execute(functionWithAssert, workflowOptions())),
      WorkflowFailedError,
    );
    // @@@SNIPEND
  });
});

// @@@SNIPSTART typescript-testing-time-skipping
import { TestWorkflowEnvironment } from '@temporalio/testing';
import { Worker } from '@temporalio/worker';
import { nanoid } from 'nanoid';
import assert from 'assert';
import { workflowFoo } from '../workflows/foo';
import { sleeperWorkflow } from '../workflows/sleeper';

describe('Skip time automatically', () => {
  let testEnv: TestWorkflowEnvironment;

  // before and after are provided by Mocha
  before(async () => {
    testEnv = await TestWorkflowEnvironment.createTimeSkipping();
  });

  after(async () => {
    await testEnv?.teardown();
  });

  it('workflowFoo', async () => {
    const worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue: 'test',
      workflowsPath: require.resolve('../workflows/foo'),
    });
    const result = await worker.runUntil(
      testEnv.client.workflow.execute(workflowFoo, {
        workflowId: nanoid(),
        taskQueue: 'test',
      }),
    );
    assert.equal(result, 'foo');
  });

  it('sleep completes almost immediately', async () => {
    const worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue: 'test',
      workflowsPath: require.resolve('../workflows/sleeper'),
    });
    // Does not wait an entire day
    await worker.runUntil(
      testEnv.client.workflow.execute(sleeperWorkflow, {
        workflowId: nanoid(),
        taskQueue: 'test',
      }),
    );
  });
});
// @@@SNIPEND

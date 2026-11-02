import { TestWorkflowEnvironment } from '@temporalio/testing';
import { Worker } from '@temporalio/worker';
import { nanoid } from 'nanoid';
import assert from 'assert';
import { daysQuery, sleeperWorkflow } from '../workflows/sleeper-days';

describe('Skip time manually', () => {
  let testEnv: TestWorkflowEnvironment;
  const taskQueue = 'test';

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createTimeSkipping();
  });

  after(async () => {
    await testEnv?.teardown();
  });

  // @@@SNIPSTART typescript-testing-manual-time-skipping
  it('sleeperWorkflow counts days correctly', async () => {
    const worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      workflowsPath: require.resolve('../workflows/sleeper-days'),
    });

    // `start()` starts the test server in "normal" mode, not skipped time mode.
    // If you don't advance time using `testEnv.sleep()`, then `sleeperWorkflow()`
    // will run for days.
    const handle = await testEnv.client.workflow.start(sleeperWorkflow, {
      workflowId: nanoid(),
      taskQueue,
    });

    await worker.runUntil(async () => {
      let numDays = await handle.query(daysQuery);
      assert.equal(numDays, 0);

      // Advance the test server's time by 25 hours
      await testEnv.sleep('25 hours');
      numDays = await handle.query(daysQuery);
      assert.equal(numDays, 1);

      await testEnv.sleep('25 hours');
      numDays = await handle.query(daysQuery);
      assert.equal(numDays, 2);

      await handle.terminate();
    });
  });
  // @@@SNIPEND
});

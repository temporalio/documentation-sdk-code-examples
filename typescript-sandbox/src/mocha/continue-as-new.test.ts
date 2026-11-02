import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import {
  assignNodesToJobUpdate,
  clusterManagerWorkflow,
  shutdownClusterUpdate,
  startClusterSignal,
} from '../workflows/continue-as-new/workflows';
import assert from 'assert';

describe('Continue-As-New', function () {
  this.timeout(60_000);
  let testEnv: TestWorkflowEnvironment;

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
  });

  after(async () => {
    await testEnv?.teardown();
  });

  it('continues as new and carries the cluster state into the next run', async () => {
    const taskQueue = 'continue-as-new';
    const worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      workflowsPath: require.resolve('../workflows/continue-as-new/workflows'),
    });

    await worker.runUntil(async () => {
      const handle = await testEnv.client.workflow.start(clusterManagerWorkflow, {
        args: [{ testContinueAsNew: true }],
        workflowId: 'continue-as-new-cluster-manager',
        taskQueue,
      });
      await handle.signal(startClusterSignal);

      // Each Update adds Events to the history, so the test hook's small history limit triggers Continue-As-New
      for (let i = 0; i < 25; i++) {
        await handle.executeUpdate(assignNodesToJobUpdate, { args: [{ numNodes: 1, jobName: `job-${i}` }] });
      }

      // The handle follows the chain, so this Update reaches the latest run
      await handle.executeUpdate(shutdownClusterUpdate);
      const result = await handle.result();
      assert.deepEqual(result, { assignedNodes: 25 });

      const firstRun = testEnv.client.workflow.getHandle(handle.workflowId, handle.firstExecutionRunId);
      assert.equal((await firstRun.describe()).status.name, 'CONTINUED_AS_NEW');
    });
  });
});

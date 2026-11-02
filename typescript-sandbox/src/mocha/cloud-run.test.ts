import { MockActivityEnvironment, TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import * as activities from '../workers/serverless-workers/cloud-run/activities';
import { myWorkflow } from '../workers/serverless-workers/cloud-run/workflows';
import assert from 'assert';

describe('Serverless Worker on GCP Cloud Run', function () {
  this.timeout(60_000);
  let testEnv: TestWorkflowEnvironment;

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
  });

  after(async () => {
    await testEnv?.teardown();
  });

  it('pins myWorkflow to the Worker Deployment Version', async () => {
    // Same versioning options as worker.ts, which connects to Temporal Cloud with TLS
    const taskQueue = 'cloud-run';
    const worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      workflowsPath: require.resolve('../workers/serverless-workers/cloud-run/workflows'),
      activities,
      workerDeploymentOptions: {
        version: { deploymentName: 'my-app', buildId: 'build-1' },
        useWorkerVersioning: true,
        defaultVersioningBehavior: 'AUTO_UPGRADE',
      },
    });
    await worker.runUntil(async () => {
      for (let i = 0; ; i++) {
        try {
          await testEnv.client.workflowService.setWorkerDeploymentCurrentVersion({
            namespace: 'default',
            deploymentName: 'my-app',
            buildId: 'build-1',
          });
          break;
        } catch (err) {
          if (i >= 50) throw err;
          await new Promise((resolve) => setTimeout(resolve, 200));
        }
      }
      const handle = await testEnv.client.workflow.start(myWorkflow, { taskQueue, workflowId: 'cloud-run' });
      assert.equal(await handle.result(), 'done');
      // setWorkflowOptions() overrides the Worker's AUTO_UPGRADE default
      const info = (await handle.describe()).raw.workflowExecutionInfo?.versioningInfo;
      assert.equal(String(info?.behavior), '1'); // VERSIONING_BEHAVIOR_PINNED
    });
  });

  it('resumes from the last Heartbeat on a retry', async () => {
    const env = new MockActivityEnvironment({ heartbeatDetails: 1 });
    const heartbeats: unknown[] = [];
    env.on('heartbeat', (d: unknown) => heartbeats.push(d));
    assert.equal(await env.run(activities.myActivity, ['first', 'second', 'third']), 'done');
    assert.deepEqual(heartbeats, [1, 2]);
  });
});

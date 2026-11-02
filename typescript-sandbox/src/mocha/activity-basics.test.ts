import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import * as activities from '../activities/basics/activities';
import { createCustomTypeWorker } from '../activities/basics/custom-type-worker';
import { createDependencyInjectionWorker } from '../activities/basics/di-worker';
import {
  customActivityTypeWorkflow,
  dependencyInjectionWorkflow,
  dynamicWorkflow,
  multipleActivitiesWorkflow,
} from '../activities/basics/workflows';
import assert from 'assert';

describe('Activity basics', function () {
  this.timeout(60_000);
  let testEnv: TestWorkflowEnvironment;

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
  });

  after(async () => {
    await testEnv?.teardown();
  });

  it('registers an Activity under a custom Activity Type', async () => {
    const worker = await createCustomTypeWorker(testEnv.nativeConnection);
    const result = await worker.runUntil(
      testEnv.client.workflow.execute(customActivityTypeWorkflow, {
        taskQueue: 'snippets',
        workflowId: 'custom-activity-type',
        args: ['Temporal'],
      }),
    );
    assert.equal(result, 'Hello, Temporal!');
  });

  it('injects dependencies into Activities', async () => {
    const worker = await createDependencyInjectionWorker(testEnv.nativeConnection);
    const result = await worker.runUntil(
      testEnv.client.workflow.execute(dependencyInjectionWorkflow, {
        taskQueue: 'dependency-injection',
        workflowId: 'dependency-injection',
      }),
    );
    assert.deepEqual(result, ['Hello: Temporal', 'Hola: Temporal']);
  });

  describe('proxying Activities', () => {
    const taskQueue = 'activity-basics';
    let worker: Worker;
    let runPromise: Promise<void>;

    before(async () => {
      worker = await Worker.create({
        connection: testEnv.nativeConnection,
        taskQueue,
        workflowsPath: require.resolve('../activities/basics/workflows'),
        activities,
      });
      runPromise = worker.run();
    });

    after(async () => {
      worker.shutdown();
      await runPromise;
    });

    it('proxies multiple Activities with the same options', async () => {
      const result = await testEnv.client.workflow.execute(multipleActivitiesWorkflow, {
        taskQueue,
        workflowId: 'multiple-activities',
        args: ['Temporal'],
      });
      assert.deepEqual(result, ['Temporal', 'act1', 'act2', 'act3']);
    });

    it('references an Activity by a dynamic name', async () => {
      const result = await testEnv.client.workflow.execute(dynamicWorkflow, {
        taskQueue,
        workflowId: 'dynamic-activities',
        args: ['act2'],
      });
      assert.equal(result, 'act2');
    });

    it('reports an unregistered Activity name on the pending Activity', async () => {
      const handle = await testEnv.client.workflow.start(dynamicWorkflow, {
        taskQueue,
        workflowId: 'dynamic-activities-missing',
        args: ['actC'],
      });
      // The failure is retryable, so the Activity keeps retrying instead of failing the Workflow
      let failure: { message?: string | null; applicationFailureInfo?: { type?: string | null } | null } | null | undefined;
      for (let i = 0; i < 100 && !failure; i++) {
        failure = (await handle.describe()).raw.pendingActivities?.[0]?.lastFailure;
        if (!failure) await new Promise((resolve) => setTimeout(resolve, 100));
      }
      assert.ok(failure?.message?.startsWith('Activity function actC is not registered on this Worker'));
      assert.equal(failure?.applicationFailureInfo?.type, 'NotFoundError');
      await handle.terminate();
    });
  });
});

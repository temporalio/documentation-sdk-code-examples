import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import { DeterminismViolationError } from '@temporalio/workflow';
import { WorkflowHandle } from '@temporalio/client';
import * as activities from '../workflows/versioning/patching/activities';
import { pizzaWorkflow, pizzaWorkflowV2 } from '../workflows/versioning/cutover/workflows';
import assert from 'assert';

type History = Awaited<ReturnType<WorkflowHandle['fetchHistory']>>;
const versionPath = (version: string) => require.resolve(`../workflows/versioning/patching/workflows-${version}`);

describe('Versioning with patching', function () {
  this.timeout(120_000);
  let testEnv: TestWorkflowEnvironment;
  const histories: Record<string, History> = {};

  // Runs myWorkflow from one version of the code to completion and keeps its Event History
  async function runVersion(version: string): Promise<History> {
    const taskQueue = `patching-${version}`;
    const worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      workflowsPath: versionPath(version),
      activities,
    });
    return worker.runUntil(async () => {
      const handle = await testEnv.client.workflow.start('myWorkflow', { taskQueue, workflowId: `patching-${version}` });
      await handle.result();
      return handle.fetchHistory();
    });
  }

  const replay = (version: string, history: History) =>
    Worker.runReplayHistory({ workflowsPath: versionPath(version) }, history);

  before(async () => {
    // The Workflows sleep for a day, so skip time
    testEnv = await TestWorkflowEnvironment.createTimeSkipping();
    histories.v1 = await runVersion('v1');
    histories.v2 = await runVersion('v2');
  });

  after(async () => {
    await testEnv?.teardown();
  });

  it('cannot replay v1 histories with the vFinal code', async () => {
    await assert.rejects(replay('vFinal', histories.v1), DeterminismViolationError);
  });

  it('replays v1 histories with the patched v2 code', async () => {
    await replay('v2', histories.v1);
  });

  it('replays v2 histories with the deprecated-patch v3 code', async () => {
    await replay('v3', histories.v2);
  });

  it('replays v3 histories with the vFinal code once the patch is removed', async () => {
    await replay('vFinal', await runVersion('v3'));
  });
});

describe('Workflow cutover', function () {
  this.timeout(60_000);
  let testEnv: TestWorkflowEnvironment;

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
  });

  after(async () => {
    await testEnv?.teardown();
  });

  it('registers both Workflow Types on one Worker', async () => {
    const taskQueue = 'pizza-tasks';
    const worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      workflowsPath: require.resolve('../workflows/versioning/cutover/workflows'),
    });
    await worker.runUntil(async () => {
      const order = { orderNumber: 'P1', pizzas: ['margherita'] };
      const v1 = await testEnv.client.workflow.execute(pizzaWorkflow, { taskQueue, workflowId: 'pizza-v1', args: [order] });
      const v2 = await testEnv.client.workflow.execute(pizzaWorkflowV2, {
        taskQueue,
        workflowId: 'pizza-v2',
        args: [{ orderNumber: 'P2', pizzas: [] }],
      });
      assert.deepEqual(v1, { orderNumber: 'P1', status: 'SUCCESS' });
      assert.deepEqual(v2, { orderNumber: 'P2', status: 'EMPTY_ORDER' });
    });
  });
});

import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import * as activities from '../activities';
import { example as developExample } from '../workflows/basics/develop-workflow';
import { example as parametersExample } from '../workflows/basics/parameters';
import { helloWorld } from '../workflows/basics/workflow-type';
import { myWorkflow } from '../workflows/basics/logging';
import { randomValues } from '../workflows/basics/random';
import { currentTime } from '../workflows/basics/current-time';
import { replayAwareWorkflow } from '../workflows/basics/replay';
import assert from 'assert';

describe('Workflow basics', function () {
  this.timeout(60_000);
  let testEnv: TestWorkflowEnvironment;

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
  });

  after(async () => {
    await testEnv?.teardown();
  });

  // Each file defines its own Workflows, and two of them share the name `example`, so each gets its own Worker.
  async function runWith<T>(workflowFile: string, fn: (taskQueue: string) => Promise<T>): Promise<T> {
    const taskQueue = `basics-${workflowFile}`;
    const worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      workflowsPath: require.resolve(`../workflows/basics/${workflowFile}`),
      activities,
    });
    return worker.runUntil(fn(taskQueue));
  }

  it('calls the greet Activity', async () => {
    const result = await runWith('develop-workflow', (taskQueue) =>
      testEnv.client.workflow.execute(developExample, {
        args: [{ name: 'Temporal' }],
        workflowId: 'basics-develop',
        taskQueue,
      }),
    );
    assert.deepEqual(result, { greeting: 'Hello, Temporal!' });
  });

  it('takes a single object parameter', async () => {
    const result = await runWith('parameters', (taskQueue) =>
      testEnv.client.workflow.execute(parametersExample, {
        args: [{ name: 'Temporal', born: 2019 }],
        workflowId: 'basics-parameters',
        taskQueue,
      }),
    );
    assert.equal(result, 'Hello Temporal, you were born in 2019.');
  });

  it('uses the function name as the Workflow Type', async () => {
    const result = await runWith('workflow-type', async (taskQueue) => {
      const handle = await testEnv.client.workflow.start(helloWorld, { workflowId: 'basics-type', taskQueue });
      assert.equal((await handle.describe()).type, 'helloWorld');
      return handle.result();
    });
    assert.equal(result, '👋 Hello World!');
  });

  it('logs with the Workflow logger', async () => {
    const result = await runWith('logging', (taskQueue) =>
      testEnv.client.workflow.execute(myWorkflow, { args: ['Temporal'], workflowId: 'basics-logging', taskQueue }),
    );
    assert.equal(result, 'Hello, Temporal!');
  });

  it('generates deterministic random values', async () => {
    const result = await runWith('random', (taskQueue) =>
      testEnv.client.workflow.execute(randomValues, { workflowId: 'basics-random', taskQueue }),
    );
    assert.ok(result.value >= 0 && result.value < 1);
    assert.match(result.id, /^[0-9a-f-]{36}$/);
  });

  it('advances the current time only after awaiting', async () => {
    await runWith('current-time', (taskQueue) =>
      testEnv.client.workflow.execute(currentTime, { workflowId: 'basics-current-time', taskQueue }),
    );
  });

  it('checks whether the Workflow is replaying', async () => {
    await runWith('replay', (taskQueue) =>
      testEnv.client.workflow.execute(replayAwareWorkflow, { workflowId: 'basics-replay', taskQueue }),
    );
  });
});

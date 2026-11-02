import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import { WorkflowFailedError } from '@temporalio/client';
import { ChildProcess, spawn } from 'child_process';
import path from 'path';
import { createTaskQueueWorker } from '../client/temporal-client/worker';
import {
  executeWithTaskQueue,
  getResult,
  getResultOrThrow,
  startWithWorkflowId,
  startWorkflow,
} from '../client/temporal-client/start-workflow';
import { failingWorkflow, subscriptionWorkflow, yourWorkflow } from '../client/temporal-client/workflows';
import assert from 'assert';

const dir = path.resolve(__dirname, '../client/temporal-client');

// Runs a sample file with ts-node in its own process and collects its output
function spawnSample(file: string, env: Record<string, string>): { child: ChildProcess; output: () => string } {
  const child = spawn(process.execPath, [require.resolve('ts-node/dist/bin'), path.join(dir, file)], {
    env: { ...process.env, ...env },
  });
  let output = '';
  child.stdout?.on('data', (d) => (output += d));
  child.stderr?.on('data', (d) => (output += d));
  return { child, output: () => output };
}

const exitOf = (child: ChildProcess) =>
  new Promise<number | null>((resolve) =>
    child.exitCode !== null ? resolve(child.exitCode) : child.once('exit', resolve),
  );

describe('Temporal Client', function () {
  this.timeout(120_000);
  let testEnv: TestWorkflowEnvironment;
  let worker: Worker;
  let runPromise: Promise<void>;

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
    worker = await createTaskQueueWorker(testEnv.nativeConnection);
    runPromise = worker.run();
  });

  after(async () => {
    worker?.shutdown();
    await runPromise;
    await testEnv?.teardown();
  });

  describe('environment configuration', () => {
    it('loads the default profile, with environment variables taking precedence', async () => {
      const { child, output } = spawnSample('env-config/load-from-file.ts', { TEMPORAL_ADDRESS: testEnv.address });
      assert.equal(await exitOf(child), 0);
      assert.match(output(), new RegExp(`Address: ${testEnv.address}`));
      assert.match(output(), /gRPC Metadata: \{"my-custom-header":"development-value","trace-id":"dev-trace-123"\}/);
      assert.match(output(), /✅ Client connected successfully!/);
    });

    it('loads the staging profile and overrides its address in code', async () => {
      const { child, output } = spawnSample('env-config/load-profile.ts', {});
      assert.equal(await exitOf(child), 0);
      assert.match(output(), /Address: localhost:7233 \(overridden from localhost:9999\)/);
      assert.match(output(), /Namespace: staging/);
    });
  });

  describe('Worker connections', () => {
    // Starts a Worker sample, runs a Workflow on its Task Queue, then stops it with SIGINT
    async function runWorkerSample(file: string, env: Record<string, string>) {
      const { child, output } = spawnSample(file, env);
      try {
        for (let i = 0; i < 300 && !/state: 'RUNNING'/.test(output()); i++) {
          if (child.exitCode !== null) break;
          await new Promise((resolve) => setTimeout(resolve, 100));
        }
        assert.match(output(), /state: 'RUNNING'/, output());
      } finally {
        child.kill('SIGINT');
        await exitOf(child);
      }
    }

    it('connects a Worker with environment variables', async () => {
      await runWorkerSample('worker-connect-env.ts', { TEMPORAL_ADDRESS: testEnv.address });
    });

    it('connects a Worker with a config file profile, overridden by environment variables', async () => {
      // The staging profile points at localhost:9999; the environment variables take precedence
      await runWorkerSample('worker-connect-config-file.ts', {
        TEMPORAL_ADDRESS: testEnv.address,
        TEMPORAL_NAMESPACE: 'default',
      });
    });
  });

  describe('Workflow Executions', () => {
    it('starts a Workflow and waits for its result', async () => {
      assert.equal(await startWorkflow(testEnv.client), 'Received argument01, argument02, argument03');
    });

    it('executes a Workflow on a Task Queue', async () => {
      assert.equal(await executeWithTaskQueue(testEnv.client), 'done');
    });

    it('starts a Workflow with a business-meaningful Workflow ID', async () => {
      // No Worker polls yourTaskQueue, so the Workflow is recorded but doesn't make progress
      const workflowId = await startWithWorkflowId(testEnv.client);
      const handle = testEnv.client.workflow.getHandle(workflowId);
      assert.equal((await handle.describe()).status.name, 'RUNNING');
      await handle.terminate();
    });

    it('gets the result of a Workflow by Workflow ID', async () => {
      await testEnv.client.workflow.start(yourWorkflow, { taskQueue: 'your-task-queue', workflowId: 'get-result' });
      assert.equal(await getResult(testEnv.client, 'get-result'), 'done');
    });

    it('wraps a WorkflowFailedError', async () => {
      await testEnv.client.workflow.start(failingWorkflow, { taskQueue: 'your-task-queue', workflowId: 'failing' });
      await assert.rejects(
        getResultOrThrow(testEnv.client, 'failing'),
        (err) =>
          err instanceof Error &&
          err.message === 'Temporal workflow failed: failing' &&
          err.cause instanceof WorkflowFailedError,
      );
    });

    it('returns a result from the Workflow', async () => {
      const result = await testEnv.client.workflow.execute(subscriptionWorkflow, {
        taskQueue: 'your-task-queue',
        workflowId: 'subscription',
        args: [[10, 20, 12]],
      });
      assert.equal(result, 'Completed subscription, Total Charged: 42');
    });
  });
});

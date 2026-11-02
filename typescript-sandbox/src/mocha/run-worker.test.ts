import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { ChildProcess, spawn } from 'child_process';
import fs from 'fs';
import path from 'path';
import { bundle } from '../workers/run-worker/scripts/build-workflow-bundle';
import { example } from '../workers/run-worker/workflows';
import assert from 'assert';

const dir = path.resolve(__dirname, '../workers/run-worker');

interface RunningWorker {
  child: ChildProcess;
  output: () => string;
}

// Runs a Worker file in its own process, as `npx ts-node <file>` would, and waits until it's polling
async function startWorker(file: string, env: Record<string, string>): Promise<RunningWorker> {
  const child = spawn(process.execPath, [require.resolve('ts-node/dist/bin'), path.join(dir, file)], {
    env: { ...process.env, ...env },
  });
  let output = '';
  child.stdout.on('data', (d) => (output += d));
  child.stderr.on('data', (d) => (output += d));
  for (let i = 0; i < 300 && !/state: 'RUNNING'/.test(output); i++) {
    if (child.exitCode !== null) throw new Error(`Worker exited early:\n${output}`);
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
  assert.match(output, /state: 'RUNNING'/, output);
  return { child, output: () => output };
}

// Sends SIGINT, as Ctrl+C does, and waits for the process to exit
async function stopWorker({ child }: RunningWorker): Promise<number | null> {
  const exited = new Promise<number | null>((resolve) => child.once('exit', (code) => resolve(code)));
  child.kill('SIGINT');
  return exited;
}

describe('Run a Worker', function () {
  this.timeout(120_000);
  let testEnv: TestWorkflowEnvironment;
  let env: Record<string, string>;
  const bundlePath = path.join(dir, 'workflow-bundle.js');

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
    env = { TEMPORAL_ADDRESS: testEnv.address };
  });

  after(async () => {
    await testEnv?.teardown();
    fs.rmSync(bundlePath, { force: true });
  });

  async function runExample(taskQueue: string, workflowId: string) {
    return testEnv.client.workflow.execute(example, { taskQueue, workflowId, args: ['Temporal'] });
  }

  it('runs a Worker that bundles Workflows from workflowsPath', async () => {
    const worker = await startWorker('worker-create.ts', env);
    try {
      assert.equal(await runExample('snippets', 'worker-create'), 'Hello, Temporal!');
    } finally {
      assert.equal(await stopWorker(worker), 0);
    }
  });

  it('runs a Worker from a prebuilt Workflow bundle', async () => {
    await bundle();
    assert.ok(fs.existsSync(bundlePath));
    const worker = await startWorker('production-worker.ts', { ...env, NODE_ENV: 'production' });
    try {
      assert.equal(await runExample('production-sample', 'production-worker'), 'Hello, Temporal!');
    } finally {
      await stopWorker(worker);
    }
  });

  it('runs a versioned Worker that pins Workflows to its version', async () => {
    const worker = await startWorker('versioned-worker.ts', env);
    try {
      // Pinned Workflows only run once the version is current
      for (let i = 0; i < 50; i++) {
        try {
          await testEnv.client.workflowService.setWorkerDeploymentCurrentVersion({
            namespace: 'default',
            deploymentName: 'my-app',
            buildId: '1.0',
          });
          break;
        } catch (err) {
          if (i === 49) throw err;
          await new Promise((resolve) => setTimeout(resolve, 200));
        }
      }
      const handle = await testEnv.client.workflow.start(example, {
        taskQueue: 'my-task-queue',
        workflowId: 'versioned-worker',
        args: ['Temporal'],
      });
      assert.equal(await handle.result(), 'Hello, Temporal!');
      const info = (await handle.describe()).raw.workflowExecutionInfo?.versioningInfo;
      assert.equal(info?.deploymentVersion?.buildId, '1.0');
      assert.equal(String(info?.behavior), '1'); // VERSIONING_BEHAVIOR_PINNED
    } finally {
      await stopWorker(worker);
    }
  });

  it('shuts down gracefully on SIGINT', async () => {
    const worker = await startWorker('graceful-worker.ts', env);
    assert.equal(await stopWorker(worker), 0);
    assert.match(worker.output(), /Worker state: STOPPED/);
  });
});

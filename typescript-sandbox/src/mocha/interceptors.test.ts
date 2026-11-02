import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { createInterceptorWorker } from '../workers/interceptors/worker';
import { createNexusWorker } from '../workers/interceptors/nexus-worker';
import { echoCallerWorkflow, greetingWorkflow } from '../workers/interceptors/workflows';
import assert from 'assert';

// Captures console.log calls while fn runs
async function captureLogs<T>(fn: () => Promise<T>): Promise<{ result: T; logs: unknown[][] }> {
  const logs: unknown[][] = [];
  const original = console.log;
  console.log = (...args: unknown[]) => void logs.push(args);
  try {
    return { result: await fn(), logs };
  } finally {
    console.log = original;
  }
}

// Captures what's written to stdout while fn runs. Workflow console.log output is written there with a Workflow prefix.
async function captureStdout<T>(fn: () => Promise<T>): Promise<{ result: T; output: string }> {
  let output = '';
  const original = process.stdout.write.bind(process.stdout);
  process.stdout.write = ((chunk: string | Uint8Array, ...rest: unknown[]) => {
    output += chunk.toString();
    return (original as (...args: unknown[]) => boolean)(chunk, ...rest);
  }) as typeof process.stdout.write;
  try {
    return { result: await fn(), output };
  } finally {
    process.stdout.write = original;
  }
}

describe('Interceptors', function () {
  this.timeout(60_000);
  let testEnv: TestWorkflowEnvironment;

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
  });

  after(async () => {
    await testEnv?.teardown();
  });

  it('logs Activities scheduled by the Workflow', async () => {
    const taskQueue = 'interceptors';
    const worker = await createInterceptorWorker(testEnv.nativeConnection, taskQueue);
    const { result, output } = await captureStdout(() =>
      worker.runUntil(
        testEnv.client.workflow.execute(greetingWorkflow, { taskQueue, workflowId: 'interceptors', args: ['Temporal'] }),
      ),
    );
    assert.equal(result, 'Hello, Temporal!');
    assert.match(output, /\[greetingWorkflow\(interceptors\)\] Starting activity \{ activityType: 'greet' \}/);
    assert.match(
      output,
      /\[greetingWorkflow\(interceptors\)\] Completed activity \{ workflow: 'greetingWorkflow', activityType: 'greet' \}/,
    );
  });

  it('logs Nexus Operations started on the Worker', async () => {
    const taskQueue = 'nexus-interceptors';
    const endpoint = 'nexus-interceptors-endpoint';
    await testEnv.connection.operatorService.createNexusEndpoint({
      spec: { name: endpoint, target: { worker: { namespace: 'default', taskQueue } } },
    });
    const worker = await createNexusWorker(testEnv.nativeConnection, taskQueue);
    const { result, logs } = await captureLogs(() =>
      worker.runUntil(
        testEnv.client.workflow.execute(echoCallerWorkflow, {
          taskQueue,
          workflowId: 'nexus-interceptors',
          args: [endpoint, 'hello'],
        }),
      ),
    );
    assert.equal(result, 'hello');
    assert.deepEqual(logs, [
      ['Starting Nexus operation', { service: 'echo', operation: 'echo' }],
      ['Nexus operation started', { service: 'echo', operation: 'echo', async: false }],
    ]);
  });
});

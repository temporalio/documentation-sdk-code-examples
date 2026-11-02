import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Client } from '@temporalio/client';
import { msToTs } from '@temporalio/common/lib/time';
import { ChildProcess, spawn } from 'child_process';
import path from 'path';
import assert from 'assert';

const dir = path.resolve(__dirname, '../nexus/quickstart');

// Runs a quickstart file with ts-node in its own process, as `npx ts-node <file>` would
function runFile(file: string, env: Record<string, string>): { child: ChildProcess; output: () => string } {
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

describe('Nexus quickstart', function () {
  this.timeout(120_000);
  let testEnv: TestWorkflowEnvironment;

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
    // Step 5: the caller Namespace and the Nexus Endpoint that routes to the handler's Task Queue
    await testEnv.client.workflowService.registerNamespace({
      namespace: 'my-caller-namespace',
      workflowExecutionRetentionPeriod: msToTs('1 day'),
    });
    await testEnv.connection.operatorService.createNexusEndpoint({
      spec: {
        name: 'my-nexus-endpoint-name',
        target: { worker: { namespace: 'default', taskQueue: 'hello-world' } },
      },
    });
  });

  after(async () => {
    await testEnv?.teardown();
  });

  it('calls the handler Workflow through the Nexus Endpoint', async () => {
    const env = { TEMPORAL_ADDRESS: testEnv.address };
    const handlerWorker = runFile('worker.ts', env);
    try {
      for (let i = 0; i < 300 && !/state: 'RUNNING'/.test(handlerWorker.output()); i++) {
        await new Promise((resolve) => setTimeout(resolve, 100));
      }
      assert.match(handlerWorker.output(), /state: 'RUNNING'/, handlerWorker.output());

      // The new Namespace may take a moment to become available to the caller
      let caller = runFile('caller-starter.ts', env);
      for (let attempt = 0; (await exitOf(caller.child)) !== 0 && attempt < 5; attempt++) {
        await new Promise((resolve) => setTimeout(resolve, 1000));
        caller = runFile('caller-starter.ts', env);
      }
      assert.match(caller.output(), /Workflow result: Hello, Temporal!/, caller.output());
    } finally {
      handlerWorker.child.kill('SIGINT');
      await exitOf(handlerWorker.child);
    }

    // The caller Workflow's Event History records the Nexus Operation
    const callerClient = new Client({
      connection: testEnv.connection,
      namespace: 'my-caller-namespace',
    });
    let workflowId: string | undefined;
    for await (const wf of callerClient.workflow.list()) workflowId = wf.workflowId;
    assert.ok(workflowId);
    const history = await callerClient.workflow.getHandle(workflowId).fetchHistory();
    for (const attrs of [
      'nexusOperationScheduledEventAttributes',
      'nexusOperationStartedEventAttributes',
      'nexusOperationCompletedEventAttributes',
    ]) {
      assert.ok(history.events?.some((e) => (e as Record<string, unknown>)[attrs]), attrs);
    }
  });
});

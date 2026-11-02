import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import { Client } from '@temporalio/client';
import { myNexusServiceHandler } from '../nexus/standalone-operations/worker/handler';
import { ENDPOINT_NAME, TASK_QUEUE } from '../nexus/standalone-operations/shared';
import { countOperations, executeEcho, listOperations, startHello } from '../nexus/standalone-operations/starter';
import assert from 'assert';

describe('Standalone Nexus Operations', function () {
  this.timeout(120_000);
  let testEnv: TestWorkflowEnvironment;
  let callerClient: Client;
  let worker: Worker;
  let runPromise: Promise<void>;

  before(async () => {
    // Same as `temporal server start-dev --namespace ... --dynamic-config-value nexusoperation.enableStandalone=true`
    testEnv = await TestWorkflowEnvironment.createLocal({
      server: {
        namespace: 'my-caller-namespace',
        extraArgs: [
          '--namespace',
          'my-handler-namespace',
          '--dynamic-config-value',
          'nexusoperation.enableStandalone=true',
        ],
      },
    });
    await testEnv.connection.operatorService.createNexusEndpoint({
      spec: {
        name: ENDPOINT_NAME,
        target: { worker: { namespace: 'my-handler-namespace', taskQueue: TASK_QUEUE } },
      },
    });
    callerClient = new Client({ connection: testEnv.connection, namespace: 'my-caller-namespace' });

    // Same options as worker/worker.ts, on the test server's connection
    worker = await Worker.create({
      connection: testEnv.nativeConnection,
      namespace: 'my-handler-namespace',
      taskQueue: TASK_QUEUE,
      workflowsPath: require.resolve('../nexus/standalone-operations/worker/workflows'),
      nexusServices: [myNexusServiceHandler],
    });
    runPromise = worker.run();
  });

  after(async () => {
    worker?.shutdown();
    await runPromise;
    await testEnv?.teardown();
  });

  it('executes a synchronous Operation', async () => {
    assert.equal(await executeEcho(callerClient), 'hello');
  });

  it('starts a Workflow-backed Operation and waits for its result', async () => {
    assert.equal(await startHello(callerClient), 'Hello, World!');
  });

  it('lists and counts Standalone Nexus Operations', async () => {
    // Visibility can lag behind the Operations that just completed
    let count = 0;
    for (let i = 0; i < 50 && (count = await countOperations(callerClient)) < 2; i++) {
      await new Promise((resolve) => setTimeout(resolve, 200));
    }
    assert.equal(count, 2);

    const lines: string[] = [];
    const original = console.log;
    console.log = (...args: unknown[]) => void lines.push(args.join(' '));
    try {
      await listOperations(callerClient);
    } finally {
      console.log = original;
    }
    assert.equal(lines.length, 2);
    assert.ok(lines.every((line) => line.endsWith('Status: COMPLETED')));
  });
});

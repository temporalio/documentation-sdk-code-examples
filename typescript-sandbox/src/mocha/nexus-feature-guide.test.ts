import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import { Client } from '@temporalio/client';
import { msToTs } from '@temporalio/common/lib/time';
import { helloServiceHandler } from '../nexus/feature-guide/service/handler';
import { helloArgsServiceHandler } from '../nexus/feature-guide/service/multiple-args';
import {
  echoCallerWorkflow,
  helloArgsCallerWorkflow,
  helloCallerWorkflow,
} from '../nexus/feature-guide/caller/workflows';
import { nexusGreetingServiceHandler } from '../nexus/feature-guide/messaging/handler';
import { getLanguagesCallerWorkflow } from '../nexus/feature-guide/messaging/caller-workflows';
import { approveSignal, greetingWorkflow } from '../nexus/feature-guide/messaging/workflows';
import assert from 'assert';

describe('Nexus feature guide', function () {
  this.timeout(120_000);
  let testEnv: TestWorkflowEnvironment;
  let callerClient: Client;
  const workers: Worker[] = [];
  const runs: Promise<void>[] = [];
  const callerTaskQueue = 'nexus-hello-caller-task-queue';

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
    for (const namespace of ['my-target-namespace', 'my-caller-namespace']) {
      await testEnv.client.workflowService.registerNamespace({
        namespace,
        workflowExecutionRetentionPeriod: msToTs('1 day'),
      });
    }
    for (const [name, taskQueue] of [
      ['my-nexus-endpoint-name', 'my-handler-task-queue'],
      ['greeting-endpoint', 'greeting-handler-task-queue'],
    ]) {
      await testEnv.connection.operatorService.createNexusEndpoint({
        spec: { name, target: { worker: { namespace: 'my-target-namespace', taskQueue } } },
      });
    }
    callerClient = new Client({ connection: testEnv.connection, namespace: 'my-caller-namespace' });

    // Same options as service/worker.ts and caller/worker.ts, on the test server's connection
    const options = [
      {
        namespace: 'my-target-namespace',
        taskQueue: 'my-handler-task-queue',
        workflowsPath: require.resolve('../nexus/feature-guide/service/workflows'),
        nexusServices: [helloServiceHandler, helloArgsServiceHandler],
      },
      {
        namespace: 'my-target-namespace',
        taskQueue: 'greeting-handler-task-queue',
        workflowsPath: require.resolve('../nexus/feature-guide/messaging/workflows'),
        nexusServices: [nexusGreetingServiceHandler],
      },
      {
        namespace: 'my-caller-namespace',
        taskQueue: callerTaskQueue,
        workflowsPath: require.resolve('./nexus-feature-guide-caller-workflows'),
      },
    ];
    // Registering a new Namespace takes a moment, so retry Worker creation until it's available
    for (const opts of options) {
      for (let i = 0; ; i++) {
        try {
          const worker = await Worker.create({ connection: testEnv.nativeConnection, ...opts });
          workers.push(worker);
          runs.push(worker.run());
          break;
        } catch (err) {
          if (i >= 20) throw err;
          await new Promise((resolve) => setTimeout(resolve, 500));
        }
      }
    }
  });

  after(async () => {
    workers.forEach((w) => w.shutdown());
    await Promise.all(runs);
    await testEnv?.teardown();
  });

  const execute = <T>(workflow: (...args: any[]) => Promise<T>, workflowId: string, args: unknown[]) =>
    callerClient.workflow.execute(workflow, { taskQueue: callerTaskQueue, workflowId, args });

  it('runs a synchronous Operation', async () => {
    assert.equal(await execute(echoCallerWorkflow, 'echo', ['This message is from the client']), 'This message is from the client');
  });

  it('runs an asynchronous Operation backed by a Workflow', async () => {
    assert.equal(await execute(helloCallerWorkflow, 'hello', ['Temporal', 'en']), 'Hello, Temporal!');
    // The handler started helloWorkflow with the business-meaningful Workflow ID
    const handlerClient = new Client({ connection: testEnv.connection, namespace: 'my-target-namespace' });
    const description = await handlerClient.workflow.getHandle('hello-Temporal-en').describe();
    assert.equal(description.type, 'helloWorkflow');
  });

  it('maps the Operation input to multiple Workflow arguments', async () => {
    assert.equal(await execute(helloArgsCallerWorkflow, 'hello-args', ['Temporal', 'fr']), 'Bonjour, Temporal!');
  });

  it('Queries a running Workflow from a synchronous Operation', async () => {
    const handlerClient = new Client({ connection: testEnv.connection, namespace: 'my-target-namespace' });
    const entity = await handlerClient.workflow.start(greetingWorkflow, {
      taskQueue: 'greeting-handler-task-queue',
      workflowId: 'GreetingWorkflow_for_user-1',
    });
    const languages = await execute(getLanguagesCallerWorkflow, 'get-languages', ['greeting-endpoint', 'user-1']);
    assert.deepEqual(languages, ['chinese', 'english']);
    await entity.signal(approveSignal);
    await entity.result();
  });
});

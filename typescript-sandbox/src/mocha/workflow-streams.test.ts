import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, afterEach, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import { WorkflowStreamClient } from '@temporalio/workflow-streams/client';
import http from 'http';
import { AddressInfo } from 'net';
import assert from 'assert';
import * as orderActivities from '../workflows/workflow-streams/order/activities';
import { orderWorkflow, StatusEvent } from '../workflows/workflow-streams/order/workflows';
import { orderWorkflowWithSleep } from '../workflows/workflow-streams/order/fixed-sleep';
import { publishStatus } from '../workflows/workflow-streams/order/publish-status';
import { watchOrder } from '../workflows/workflow-streams/order/watch-order';
import { watchStatusAndProgress } from '../workflows/workflow-streams/order/watch-topics';
import * as deltaActivities from '../workflows/workflow-streams/deltas/activities';
import { deltasWorkflow, doneSignal } from '../workflows/workflow-streams/deltas/workflows';
import * as chatActivities from '../workflows/workflow-streams/chat/activities';
import { chatWorkflow } from '../workflows/workflow-streams/chat/workflows';
import { streamChat } from '../workflows/workflow-streams/chat/consumer';
import { longRunningWorkflow } from '../workflows/workflow-streams/long-running/workflows';
import { longRunningWorkflow as longRunningWithOptions } from '../workflows/workflow-streams/long-running/with-options';

// Captures console.log output while fn runs
async function captureLogs(fn: () => Promise<unknown>): Promise<string[]> {
  const lines: string[] = [];
  const original = console.log;
  console.log = (...args: unknown[]) => void lines.push(args.join(' '));
  try {
    await fn();
  } finally {
    console.log = original;
  }
  return lines;
}

async function waitFor(check: () => boolean | Promise<boolean>): Promise<void> {
  for (let i = 0; i < 200 && !(await check()); i++) {
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
}

describe('Workflow Streams', function () {
  this.timeout(60_000);
  let testEnv: TestWorkflowEnvironment;
  let worker: Worker;
  let runPromise: Promise<void>;
  let openaiServer: http.Server;
  let completionRequests = 0;
  const taskQueue = 'workflow-streams';

  before(async () => {
    // A local stand-in for the OpenAI API: fails the first request, then streams a completion
    openaiServer = http.createServer((req, res) => {
      completionRequests++;
      if (completionRequests === 1) {
        res.writeHead(500, { 'content-type': 'application/json' });
        res.end(JSON.stringify({ error: { message: 'simulated outage' } }));
        return;
      }
      res.writeHead(200, { 'content-type': 'text/event-stream' });
      for (const content of ['Durable ', 'execution ', 'streams.']) {
        res.write(`data: ${JSON.stringify({ choices: [{ index: 0, delta: { content } }] })}\n\n`);
      }
      res.end('data: [DONE]\n\n');
    });
    await new Promise<void>((resolve) => openaiServer.listen(0, '127.0.0.1', resolve));
    process.env.OPENAI_BASE_URL = `http://127.0.0.1:${(openaiServer.address() as AddressInfo).port}/v1`;
    process.env.OPENAI_API_KEY = 'test-key';

    testEnv = await TestWorkflowEnvironment.createLocal();
    worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      workflowsPath: require.resolve('./workflow-streams-workflows'),
      activities: { ...orderActivities, ...deltaActivities, ...chatActivities },
    });
    runPromise = worker.run();
  });

  after(async () => {
    worker?.shutdown();
    await runPromise;
    await testEnv?.teardown();
    openaiServer?.close();
  });

  it('publishes status events from a Workflow', async () => {
    await testEnv.client.workflow.execute(orderWorkflow, {
      taskQueue,
      workflowId: 'order-1',
      args: [{ orderId: 'order-1' }],
    });
  });

  it('subscribes to a typed topic until the terminator arrives', async () => {
    const handle = await testEnv.client.workflow.start(orderWorkflowWithSleep, {
      taskQueue,
      workflowId: 'order-2',
      args: [{ orderId: 'order-2' }],
    });
    const lines = await captureLogs(() => watchOrder(testEnv.client, 'order-2'));
    assert.deepEqual(lines, [
      '[  0%] validating: checking inventory',
      '[ 50%] shipping: dispatching to warehouse',
      '[100%] completed: ',
    ]);
    await handle.terminate();
  });

  it('publishes to a Workflow stream from a Client', async () => {
    const handle = await testEnv.client.workflow.start(orderWorkflowWithSleep, {
      taskQueue,
      workflowId: 'order-3',
      args: [{ orderId: 'order-3' }],
    });
    await publishStatus(testEnv.client, 'order-3');
    const states: string[] = [];
    for await (const item of WorkflowStreamClient.create(testEnv.client, 'order-3').topic<StatusEvent>('status').subscribe()) {
      states.push(item.data.state);
      if (states.includes('started') && states.includes('completed')) break;
    }
    assert.ok(states.includes('started'));
    await handle.terminate();
  });

  it('subscribes to topics with different payload types', async () => {
    const handle = await testEnv.client.workflow.start(orderWorkflowWithSleep, {
      taskQueue,
      workflowId: 'order-4',
      args: [{ orderId: 'order-4' }],
    });
    const lines = await captureLogs(async () => {
      const watching = watchStatusAndProgress(testEnv.client, 'order-4');
      // The loop ends when the Workflow closes
      await waitFor(async () => (await handle.describe()).status.name === 'RUNNING');
      await new Promise((resolve) => setTimeout(resolve, 1500));
      await handle.terminate();
      await watching;
    });
    assert.deepEqual(lines, [
      '[status] validating: checking inventory',
      '[progress] inventory reserved',
      '[status] shipping: dispatching to warehouse',
      '[status] completed: ',
    ]);
  });

  for (const [publisher, expected] of [
    ['streamDeltas', 'Order o-5 is on its way.'],
    ['streamDeltasWithForceFlush', 'Order o-5 is on its way.'],
    ['streamInPhases', 'phase one, part two. phase two after checkpoint-1.'],
  ] as const) {
    it(`publishes from an Activity (${publisher})`, async () => {
      const workflowId = `deltas-${publisher}`;
      const handle = await testEnv.client.workflow.start(deltasWorkflow, {
        taskQueue,
        workflowId,
        args: ['o-5', publisher],
      });
      let text = '';
      for await (const item of WorkflowStreamClient.create(testEnv.client, workflowId)
        .topic<deltaActivities.Delta>('delta')
        .subscribe()) {
        text += item.data.text;
        if (text === expected) break;
      }
      assert.equal(text, expected);
      await handle.signal(doneSignal);
      await handle.result();
    });
  }

  it('streams LLM output and resets on a retried attempt', async () => {
    const handle = await testEnv.client.workflow.start(chatWorkflow, {
      taskQueue,
      workflowId: 'chat-1',
      args: [{ prompt: 'Say something about Temporal' }],
    });
    const output = await streamChat(testEnv.client, 'chat-1');
    assert.equal(output, 'Durable execution streams.');
    assert.equal(await handle.result(), 'Durable execution streams.');
    assert.equal(completionRequests, 2);
  });
});

describe('Workflow Streams with Continue-As-New', function () {
  this.timeout(120_000);
  let testEnv: TestWorkflowEnvironment;
  const workers: Worker[] = [];
  const runs: Promise<void>[] = [];

  before(async () => {
    // Suggest Continue-As-New after a short history so the tests reach a rollover quickly
    testEnv = await TestWorkflowEnvironment.createLocal({
      server: { extraArgs: ['--dynamic-config-value', 'limit.historyCount.suggestContinueAsNew=20'] },
    });
    for (const [taskQueue, file] of [
      ['long-running', 'workflows'],
      ['long-running-options', 'with-options'],
      ['other-tq', 'with-options'],
    ]) {
      const worker = await Worker.create({
        connection: testEnv.nativeConnection,
        taskQueue,
        workflowsPath: require.resolve(`../workflows/workflow-streams/long-running/${file}`),
      });
      workers.push(worker);
      runs.push(worker.run());
    }
  });

  afterEach(async () => {
    for await (const wf of testEnv.client.workflow.list({ query: 'ExecutionStatus="Running"' })) {
      await testEnv.client.workflow.getHandle(wf.workflowId).terminate();
    }
  });

  after(async () => {
    workers.forEach((w) => w.shutdown());
    await Promise.all(runs);
    await testEnv?.teardown();
  });

  it('carries the stream across Continue-As-New without gaps', async () => {
    const handle = await testEnv.client.workflow.start(longRunningWorkflow, {
      taskQueue: 'long-running',
      workflowId: 'long-running',
      args: [{ itemsProcessed: 0 }],
    });
    const offsets: number[] = [];
    for await (const item of WorkflowStreamClient.create(testEnv.client, 'long-running').topic('tick').subscribe()) {
      offsets.push(item.offset);
      if (offsets.length >= 12) break;
    }
    const firstRun = testEnv.client.workflow.getHandle('long-running', handle.firstExecutionRunId);
    assert.equal((await firstRun.describe()).status.name, 'CONTINUED_AS_NEW');
    // Offsets keep counting up from the previous run
    assert.deepEqual(offsets, [...Array(12).keys()]);
  });

  it('continues as new onto another Task Queue', async () => {
    await testEnv.client.workflow.start(longRunningWithOptions, {
      taskQueue: 'long-running-options',
      workflowId: 'long-running-options',
      args: [{ itemsProcessed: 0 }],
    });
    const latest = testEnv.client.workflow.getHandle('long-running-options');
    await waitFor(async () => (await latest.describe()).taskQueue === 'other-tq');
    assert.equal((await latest.describe()).taskQueue, 'other-tq');
  });
});

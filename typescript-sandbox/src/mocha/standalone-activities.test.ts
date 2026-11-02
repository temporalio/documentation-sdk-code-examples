import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import * as activities from '../activities/standalone/quickstart/activities';
import { run } from '../activities/standalone/quickstart/execute';
import { run as runStart } from '../activities/standalone/start';
import { run as runList } from '../activities/standalone/list';
import assert from 'assert';

describe('Standalone Activities', function () {
  this.timeout(60_000);
  let testEnv: TestWorkflowEnvironment;
  let worker: Worker;
  let runPromise: Promise<void>;

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
    // Same registration as worker.ts, on the test server's connection
    worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue: 'hello-standalone-activities',
      activities,
    });
    runPromise = worker.run();
  });

  after(async () => {
    worker?.shutdown();
    await runPromise;
    await testEnv?.teardown();
  });

  // Runs one of the sample programs and returns what it logged.
  // loadClientConnectConfig() in the samples reads the address from the environment.
  async function captureRun(fn: () => Promise<void>): Promise<string[]> {
    process.env.TEMPORAL_ADDRESS = testEnv.address;
    const lines: string[] = [];
    const original = console.log;
    console.log = (...args: unknown[]) => void lines.push(args.join(' '));
    try {
      await fn();
    } finally {
      console.log = original;
      delete process.env.TEMPORAL_ADDRESS;
    }
    return lines;
  }

  it('executes the Activity with and without type checking', async () => {
    assert.deepEqual(await captureRun(run), ['Hello, World!', 'Oops! name must be a string']);
  });

  it('starts an Activity, waits for its result, and gets a new handle', async () => {
    assert.deepEqual(await captureRun(runStart), ['Hello, Temporal!', 'Hello, Temporal!']);
  });

  it('lists and counts Standalone Activities', async () => {
    // The two tests above ran three Activities on this Task Queue; wait for Visibility to catch up
    const query = 'TaskQueue="hello-standalone-activities"';
    for (let i = 0; i < 50 && (await testEnv.client.activity.count(query)).count < 3; i++) {
      await new Promise((resolve) => setTimeout(resolve, 200));
    }
    const lines = await captureRun(runList);
    assert.equal(lines.filter((line) => line.includes(' | greet | ')).length, 3);
    assert.equal(lines[lines.length - 1], 'Total activities: 3');
  });
});

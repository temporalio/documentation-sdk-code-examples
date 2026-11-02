import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import * as activities from '../activities/standalone/quickstart/activities';
import { run } from '../activities/standalone/quickstart/execute';
import assert from 'assert';

describe('Standalone Activities quickstart', function () {
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

  it('executes the Activity with and without type checking', async () => {
    // loadClientConnectConfig() in execute.ts reads the address from the environment
    process.env.TEMPORAL_ADDRESS = testEnv.address;
    const lines: string[] = [];
    const original = console.log;
    console.log = (...args: unknown[]) => void lines.push(args.join(' '));
    try {
      await run();
    } finally {
      console.log = original;
      delete process.env.TEMPORAL_ADDRESS;
    }
    assert.deepEqual(lines, ['Hello, World!', 'Oops! name must be a string']);
  });
});

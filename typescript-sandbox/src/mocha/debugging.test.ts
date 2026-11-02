import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { bundleWorkflowCode, Worker } from '@temporalio/worker';
import { yourWorkflow } from '../best-practices/debugging/workflows';
import assert from 'assert';

const dir = '../best-practices/debugging';
const quietLogger = { trace() {}, debug() {}, info() {}, warn() {}, error() {}, log() {} };

describe('Debugging', function () {
  this.timeout(60_000);

  // The bundler names the Node.js module the Workflow code reaches: fs directly, or http through the Activity
  for (const [file, module] of [
    ['antipattern-fs', 'fs'],
    ['antipattern-direct-activity', 'http'],
  ]) {
    it(`fails to bundle Workflow code that imports Node.js modules (${file})`, async () => {
      await assert.rejects(
        bundleWorkflowCode({ workflowsPath: require.resolve(`${dir}/${file}`), logger: quietLogger }),
        (err: Error) =>
          err.message.includes('is importing the following disallowed modules') && err.message.includes(`'${module}'`),
      );
    });
  }

  describe('proxyActivities', () => {
    let testEnv: TestWorkflowEnvironment;

    before(async () => {
      testEnv = await TestWorkflowEnvironment.createLocal();
    });

    after(async () => {
      await testEnv?.teardown();
    });

    it('calls the Activity through proxyActivities', async () => {
      const worker = await Worker.create({
        connection: testEnv.nativeConnection,
        taskQueue: 'debugging',
        workflowsPath: require.resolve(`${dir}/workflows`),
        // Mock the Activity so the test doesn't make a network call
        activities: { makeHTTPRequest: async (url: string) => `fetched ${url}` },
      });
      const result = await worker.runUntil(
        testEnv.client.workflow.execute(yourWorkflow, { taskQueue: 'debugging', workflowId: 'debugging' }),
      );
      assert.equal(result, 'fetched https://temporal.io');
    });
  });
});

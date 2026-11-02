// @@@SNIPSTART typescript-testing-mock-activities
import type * as activities from '../activities';
import { TestWorkflowEnvironment } from '@temporalio/testing';
import { Worker } from '@temporalio/worker';
import assert from 'assert';
import { httpWorkflow } from '../workflows/http';

describe('Mock Activities', () => {
  let testEnv: TestWorkflowEnvironment;

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
  });

  after(async () => {
    await testEnv?.teardown();
  });

  it('uses the mocked makeHTTPRequest Activity', async () => {
    // Creating a mock object of the activities.
    const mockActivities: Partial<typeof activities> = {
      makeHTTPRequest: async () => '99',
    };

    // Creating a worker with the mocked activities.
    const worker = await Worker.create({
      activities: mockActivities,
      connection: testEnv.nativeConnection,
      taskQueue: 'test',
      workflowsPath: require.resolve('../workflows/http'),
    });

    const result = await worker.runUntil(
      testEnv.client.workflow.execute(httpWorkflow, { workflowId: 'http', taskQueue: 'test' }),
    );
    assert.equal(result, 'The answer is 99');
  });
});
// @@@SNIPEND

import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import { defaultPayloadConverter } from '@temporalio/common';
import * as activities from '../platform/enriching-ui/activities';
import { executeWithSummary, startWithSummary } from '../platform/enriching-ui/client';
import { yourWorkflow as activitySummaryWorkflow } from '../platform/enriching-ui/activity-summary';
import { yourWorkflow as timerSummaryWorkflow } from '../platform/enriching-ui/timer-summary';
import assert from 'assert';

const decode = (payload: unknown) => defaultPayloadConverter.fromPayload<string>(payload as never);

describe('Enriching the UI', function () {
  this.timeout(120_000);

  // Each file defines yourWorkflow, so each gets its own Worker
  async function runWith<T>(
    testEnv: TestWorkflowEnvironment,
    file: string,
    taskQueue: string,
    fn: () => Promise<T>,
  ): Promise<T> {
    const worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      workflowsPath: require.resolve(`../platform/enriching-ui/${file}`),
      activities,
    });
    return worker.runUntil(fn());
  }

  describe('with a local server', () => {
    let testEnv: TestWorkflowEnvironment;

    before(async () => {
      testEnv = await TestWorkflowEnvironment.createLocal();
    });

    after(async () => {
      await testEnv?.teardown();
    });

    it('starts a Workflow with a static summary and details, and sets current details', async () => {
      await runWith(testEnv, 'current-details', 'your-task-queue', async () => {
        const handle = await startWithSummary(testEnv.client);
        assert.equal(await handle.result(), 'Workflow completed');
        const description = await handle.describe();
        assert.equal(await description.staticSummary(), 'Order processing for customer #12345');
        assert.equal(await description.staticDetails(), 'Processing premium order with expedited shipping');
        // The Web UI reads current details through this built-in Query
        const metadata = await handle.query<{ currentDetails?: string }>('__temporal_workflow_metadata');
        assert.equal(metadata.currentDetails, 'Updated workflow details with new status');
      });
    });

    it('executes a Workflow with a static summary and details', async () => {
      const result = await runWith(testEnv, 'current-details', 'your-task-queue', () =>
        executeWithSummary(testEnv.client),
      );
      assert.equal(result, 'Workflow completed');
    });

    it('attaches a summary to an Activity', async () => {
      await runWith(testEnv, 'activity-summary', 'activity-summary', async () => {
        const handle = await testEnv.client.workflow.start(activitySummaryWorkflow, {
          taskQueue: 'activity-summary',
          workflowId: 'activity-summary',
          args: ['user-1'],
        });
        assert.equal(await handle.result(), 'Processed user-1');
        const history = await handle.fetchHistory();
        const scheduled = history.events?.find((e) => e.activityTaskScheduledEventAttributes);
        assert.equal(decode(scheduled?.userMetadata?.summary), 'Processing user data');
      });
    });
  });

  describe('with time skipping', () => {
    let testEnv: TestWorkflowEnvironment;

    before(async () => {
      testEnv = await TestWorkflowEnvironment.createTimeSkipping();
    });

    after(async () => {
      await testEnv?.teardown();
    });

    it('attaches a summary to a Timer', async () => {
      await runWith(testEnv, 'timer-summary', 'timer-summary', async () => {
        const handle = await testEnv.client.workflow.start(timerSummaryWorkflow, {
          taskQueue: 'timer-summary',
          workflowId: 'timer-summary',
          args: ['order-1'],
        });
        assert.equal(await handle.result(), 'Timer completed');
        const history = await handle.fetchHistory();
        const started = history.events?.find((e) => e.timerStartedEventAttributes);
        assert.equal(decode(started?.userMetadata?.summary), 'Waiting for payment confirmation');
      });
    });
  });
});

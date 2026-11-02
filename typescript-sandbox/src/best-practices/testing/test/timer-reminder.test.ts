import { TestWorkflowEnvironment } from '@temporalio/testing';
import { Worker } from '@temporalio/worker';
import { nanoid } from 'nanoid';
import assert from 'assert';
import type { createActivities } from '../timer/activities';
import { processOrderWorkflow } from '../timer/workflows';

const DAY_MS = 24 * 60 * 60 * 1000;

describe('Skip time in Activities', () => {
  let env: TestWorkflowEnvironment;

  before(async () => {
    env = await TestWorkflowEnvironment.createTimeSkipping();
  });

  after(async () => {
    await env?.teardown();
  });

  // @@@SNIPSTART typescript-testing-timer-reminder-test
  it('sends reminder email if processOrder does not complete in time', async () => {
    // This test doesn't actually take days to complete: the TestWorkflowEnvironment starts the
    // Test Server, which automatically skips time when there are no running Activities.
    let emailSent = false;
    const mockActivities: ReturnType<typeof createActivities> = {
      async processOrder() {
        // Test server switches to "normal" time while an Activity is executing.
        // Call `env.sleep` to skip ahead 2 days, by which time sendNotificationEmail
        // should have been called.
        await env.sleep('2 days');
      },
      async sendNotificationEmail() {
        emailSent = true;
      },
    };
    const worker = await Worker.create({
      connection: env.nativeConnection,
      taskQueue: 'test',
      workflowsPath: require.resolve('../timer/workflows'),
      activities: mockActivities,
    });
    await worker.runUntil(
      env.client.workflow.execute(processOrderWorkflow, {
        workflowId: nanoid(),
        taskQueue: 'test',
        args: [{ orderProcessingMS: 3 * DAY_MS, sendDelayedEmailTimeoutMS: DAY_MS }],
      }),
    );
    assert.ok(emailSent);
  });
  // @@@SNIPEND
});

import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import { WorkflowUpdateFailedError } from '@temporalio/client';
import assert from 'assert';
import * as greetingActivities from '../workflows/message-passing/introduction/activities';
import { runGreetingClient } from '../workflows/message-passing/introduction/client';
import {
  greetingWorkflow,
  Language,
  setLanguage,
  setLanguageUsingActivity,
  approve,
} from '../workflows/message-passing/introduction/workflows';
import { yourWorkflow } from '../workflows/message-passing/signals/other-workflow';
import { yourWorkflowThatSignals } from '../workflows/message-passing/signals/external-signal';
import { signalWithStart } from '../workflows/message-passing/signals/signal-with-start';
import { runTransaction } from '../workflows/message-passing/update-with-start/client';
import * as patternActivities from '../workflows/message-passing/patterns/activities';
import { finishSignal, mySignal, myUpdate } from '../workflows/message-passing/patterns/shared';
import { waitInHandlerWorkflow } from '../workflows/message-passing/patterns/wait-in-handler';
import { myWorkflow as waitForHandlersWorkflow } from '../workflows/message-passing/patterns/wait-for-handlers';
import { myWorkflow as concurrencyBugWorkflow } from '../workflows/message-passing/patterns/concurrency-bug';
import { myWorkflow as concurrencyLockWorkflow } from '../workflows/message-passing/patterns/concurrency-lock';
import { isBlockedQuery, unblockOrCancel, unblockSignal } from '../workflows/message-passing/static-dynamic/static';
import { dynamicSignalsWorkflow, handledQuery } from '../workflows/message-passing/static-dynamic/dynamic';
import { MySignal, handledByQuery, replaceHandlerWorkflow } from '../workflows/message-passing/static-dynamic/replace-handler';

describe('Workflow message passing', function () {
  this.timeout(60_000);
  let testEnv: TestWorkflowEnvironment;

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
  });

  after(async () => {
    await testEnv?.teardown();
  });

  // Several files define Workflows with the same name, so each file gets its own Worker
  async function runWith<T>(
    workflowFile: string,
    fn: (taskQueue: string) => Promise<T>,
    options: { taskQueue?: string; activities?: object } = {},
  ): Promise<T> {
    const taskQueue = options.taskQueue ?? `message-passing-${workflowFile.replace('/', '-')}`;
    const worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      workflowsPath: require.resolve(`../workflows/message-passing/${workflowFile}`),
      activities: options.activities,
    });
    return worker.runUntil(fn(taskQueue));
  }

  it('sends a Query, Updates, and a Signal from a Client', async () => {
    const result = await runWith(
      'introduction/workflows',
      (taskQueue) => runGreetingClient(testEnv.client, taskQueue),
      { activities: greetingActivities },
    );
    assert.equal(result, 'Hello, world');
  });

  it('rejects an unsupported language in the validator and adds it with an Activity', async () => {
    const result = await runWith(
      'introduction/workflows',
      async (taskQueue) => {
        const handle = await testEnv.client.workflow.start(greetingWorkflow, {
          taskQueue,
          workflowId: 'greeting-workflow-async',
        });
        await assert.rejects(
          handle.executeUpdate(setLanguage, { args: [Language.FRENCH] }),
          WorkflowUpdateFailedError,
        );
        const previous = await handle.executeUpdate(setLanguageUsingActivity, { args: [Language.FRENCH] });
        assert.equal(previous, Language.ENGLISH);
        await handle.signal(approve, { name: 'me' });
        return handle.result();
      },
      { activities: greetingActivities },
    );
    assert.equal(result, 'Bonjour, monde');
  });

  it('sends a Signal from a Workflow and with Signal-With-Start', async () => {
    await runWith(
      'signals/workflows',
      async (taskQueue) => {
        const receiver = await testEnv.client.workflow.start(yourWorkflow, {
          taskQueue,
          workflowId: 'workflow-id-123',
          args: [{ foo: 1 }],
        });
        await testEnv.client.workflow.execute(yourWorkflowThatSignals, {
          taskQueue,
          workflowId: 'workflow-that-signals',
        });
        assert.deepEqual(await receiver.result(), [{ userId: 'user-1', groupId: 'group-1' }]);

        await signalWithStart(testEnv.client);
        const started = testEnv.client.workflow.getHandle('workflow-id-123');
        assert.deepEqual(await started.result(), [{ userId: 'user-1', groupId: 'group-1' }]);
      },
      { taskQueue: 'my-taskqueue' },
    );
  });

  it('returns an Update result before the Workflow completes with Update-With-Start', async () => {
    const { earlyConfirmation, finalReport } = await runWith(
      'update-with-start/workflows',
      () => runTransaction(testEnv.client, 'transaction-1', 'tx-1'),
      { taskQueue: 'early-return' },
    );
    assert.deepEqual(earlyConfirmation, { id: 'tx-1', status: 'confirmed' });
    assert.deepEqual(finalReport, { id: 'tx-1', status: 'complete', finalAmount: 77 });
  });

  it('waits inside an Update handler until the Workflow is ready', async () => {
    await runWith('patterns/wait-in-handler', async (taskQueue) => {
      const handle = await testEnv.client.workflow.start(waitInHandlerWorkflow, {
        taskQueue,
        workflowId: 'wait-in-handler',
      });
      const output = await handle.executeUpdate(myUpdate, { args: [{ name: 'Temporal' }] });
      assert.deepEqual(output, { greeting: 'Hello, Temporal!' });
      await handle.result();
    });
  });

  it('waits for handlers to finish before completing', async () => {
    const result = await runWith(
      'patterns/wait-for-handlers',
      async (taskQueue) => {
        const handle = await testEnv.client.workflow.start(waitForHandlersWorkflow, {
          taskQueue,
          workflowId: 'wait-for-handlers',
        });
        await handle.signal(mySignal);
        await handle.signal(finishSignal);
        return handle.result();
      },
      { activities: patternActivities },
    );
    assert.ok(result.x > 0 && result.x === result.y);
  });

  for (const [file, workflow] of [
    ['concurrency-bug', concurrencyBugWorkflow],
    ['concurrency-lock', concurrencyLockWorkflow],
  ] as const) {
    it(`runs concurrent Signal handlers (${file})`, async () => {
      const result = await runWith(
        `patterns/${file}`,
        async (taskQueue) => {
          const handle = await testEnv.client.workflow.start(workflow, { taskQueue, workflowId: file });
          await handle.signal(mySignal);
          await handle.signal(mySignal);
          await handle.signal(finishSignal);
          return handle.result();
        },
        { activities: patternActivities },
      );
      assert.equal(result.x, result.y);
    });
  }

  it('defines a Signal and Query statically', async () => {
    await runWith('static-dynamic/static', async (taskQueue) => {
      const handle = await testEnv.client.workflow.start(unblockOrCancel, { taskQueue, workflowId: 'static' });
      assert.equal(await handle.query(isBlockedQuery), true);
      await handle.signal(unblockSignal);
      await handle.result();
    });
  });

  it('defines Signals dynamically', async () => {
    const result = await runWith('static-dynamic/dynamic', async (taskQueue) => {
      const handle = await testEnv.client.workflow.start(dynamicSignalsWorkflow, {
        taskQueue,
        workflowId: 'dynamic',
        args: ['a', 'b'],
      });
      // A Query runs after the first Workflow Task, so every handler is registered before the Signals arrive.
      // Signals that arrive earlier are buffered and delivered to the first handler registered for them.
      await handle.query(handledQuery);
      await handle.signal('genericSignal', { taskId: 'a' });
      await handle.signal('task-a', { taskId: 'a' });
      // The helper replaced the earlier inline handler for task-b
      await handle.signal('task-b', { taskId: 'b' });
      return handle.result();
    });
    assert.deepEqual(result, ['genericSignal: task A', 'inline: a', 'helper: b']);
  });

  it('replaces a handler when setHandler is called again', async () => {
    const result = await runWith('static-dynamic/replace-handler', async (taskQueue) => {
      const handle = await testEnv.client.workflow.start(replaceHandlerWorkflow, { taskQueue, workflowId: 'replace' });
      await handle.query(handledByQuery);
      await handle.signal(MySignal);
      return handle.result();
    });
    assert.equal(result, 'handlerFn2');
  });
});

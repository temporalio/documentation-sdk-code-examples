import { TestWorkflowEnvironment } from '@temporalio/testing';
import { WorkflowFailedError } from '@temporalio/client';
import { ApplicationFailure } from '@temporalio/common';
import { Worker } from '@temporalio/worker';
import { after, before, describe, it } from 'mocha';
import assert from 'assert';
import { openAccount } from '../design-patterns/saga/workflows';
import type * as sagaActivities from '../design-patterns/saga/activities';
import type { OpenAccountRequest } from '../design-patterns/saga/shared';

const req: OpenAccountRequest = {
  accountId: 'acct-1',
  address: '1 Main St',
  clientEmail: 'client@example.com',
  bankAccountId: 'bank-1',
};

// Records the order Activities run in. `failOn` names the forward step that fails.
function recordingActivities(calls: string[], failOn?: string): typeof sagaActivities {
  const step = (name: string) => async () => {
    calls.push(name);
    if (name === failOn) {
      throw ApplicationFailure.nonRetryable(`${name} failed`);
    }
  };
  return {
    createAccount: step('createAccount'),
    addAddress: step('addAddress'),
    addClient: step('addClient'),
    addBankAccount: step('addBankAccount'),
    clearPostalAddresses: step('clearPostalAddresses'),
    removeClient: step('removeClient'),
    disconnectBankAccounts: step('disconnectBankAccounts'),
  };
}

describe('Saga design pattern', () => {
  let testEnv: TestWorkflowEnvironment;

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
  });

  after(async () => {
    await testEnv?.teardown();
  });

  async function run(calls: string[], failOn?: string, workflowId = 'saga-test') {
    const { client, nativeConnection } = testEnv;
    const taskQueue = 'saga-test-' + workflowId;
    const worker = await Worker.create({
      connection: nativeConnection,
      taskQueue,
      workflowsPath: require.resolve('../design-patterns/saga/workflows'),
      activities: recordingActivities(calls, failOn),
    });
    return worker.runUntil(client.workflow.execute(openAccount, { args: [req], workflowId, taskQueue }));
  }

  it('completes when every step succeeds', async () => {
    const calls: string[] = [];
    const result = await run(calls, undefined, 'saga-success');
    assert.equal(result, 'Account acct-1 opened');
    assert.deepEqual(calls, ['createAccount', 'addAddress', 'addClient', 'addBankAccount']);
  });

  it('runs compensations in reverse order and re-throws when the last step fails', async () => {
    const calls: string[] = [];
    await assert.rejects(run(calls, 'addBankAccount', 'saga-fail-last'), WorkflowFailedError);
    assert.deepEqual(calls, [
      'createAccount',
      'addAddress',
      'addClient',
      'addBankAccount',
      'disconnectBankAccounts',
      'removeClient',
      'clearPostalAddresses',
    ]);
  });

  it('runs only the compensations registered before a failed middle step', async () => {
    const calls: string[] = [];
    await assert.rejects(run(calls, 'addClient', 'saga-fail-middle'), WorkflowFailedError);
    assert.deepEqual(calls, ['createAccount', 'addAddress', 'addClient', 'removeClient', 'clearPostalAddresses']);
  });

  it('runs no compensations when the first step fails', async () => {
    const calls: string[] = [];
    await assert.rejects(run(calls, 'createAccount', 'saga-fail-first'), WorkflowFailedError);
    assert.deepEqual(calls, ['createAccount']);
  });
});

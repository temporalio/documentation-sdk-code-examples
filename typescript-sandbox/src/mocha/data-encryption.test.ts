import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import { Client } from '@temporalio/client';
import { decode } from '@temporalio/common/lib/encoding';
import assert from 'assert';
import { getDataConverter } from '../best-practices/data-handling/data-encryption/data-converter';
import { example } from '../best-practices/data-handling/data-encryption/workflows';

describe('Payload encryption', function () {
  this.timeout(60_000);
  let testEnv: TestWorkflowEnvironment;

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
  });

  after(async () => {
    await testEnv?.teardown();
  });

  it('encrypts Payloads on the Client and Worker, so the server stores only ciphertext', async () => {
    const taskQueue = 'encryption';
    const worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      workflowsPath: require.resolve('../best-practices/data-handling/data-encryption/workflows'),
      dataConverter: await getDataConverter(),
    });
    const client = new Client({ connection: testEnv.connection, dataConverter: await getDataConverter() });

    // The codec logs each Payload it decrypts
    const original = console.log;
    console.log = () => undefined;
    let result: string;
    try {
      result = await worker.runUntil(
        client.workflow.execute(example, {
          args: ['Alice: Private message for Bob.'],
          taskQueue,
          workflowId: 'encryption',
        }),
      );
    } finally {
      console.log = original;
    }
    assert.equal(result, "Alice: Private message for Bob.\nBob: Hi Alice, I'm Workflow Bob.");

    // The Event History, which is what the server stores, holds only the encrypted Payload
    const history = await testEnv.client.workflow.getHandle('encryption').fetchHistory();
    const input = history.events?.[0].workflowExecutionStartedEventAttributes?.input?.payloads?.[0];
    assert.equal(decode(input!.metadata!.encoding), 'binary/encrypted');
    assert.equal(decode(input!.metadata!['encryption-key-id']), 'test-key-id');
    assert.ok(!Buffer.from(input!.data!).toString().includes('Private message'));
  });
});

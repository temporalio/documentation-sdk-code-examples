import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import { Client } from '@temporalio/client';
import { decode } from '@temporalio/common/lib/encoding';
import assert from 'assert';
import { example as ejsonExample } from '../best-practices/data-handling/data-conversion/ejson/workflows';
import type { User } from '../best-practices/data-handling/data-conversion/ejson/types';
import { example as protoExample } from '../best-practices/data-handling/data-conversion/protobufs/src/workflows';
import * as protoActivities from '../best-practices/data-handling/data-conversion/protobufs/src/activities';
import { foo, ProtoResult } from '../best-practices/data-handling/data-conversion/protobufs/protos/root';
import { payloadConverter as binaryConverter } from '../best-practices/data-handling/data-conversion/protobufs/src/binary-payload-converter';
import { payloadConverter as compositeConverter } from '../best-practices/data-handling/data-conversion/protobufs/src/composite-payload-converter';

const dir = '../best-practices/data-handling/data-conversion';
const encodingOf = (payload: { metadata?: Record<string, Uint8Array> | null } | undefined) =>
  decode(payload!.metadata!.encoding);

describe('Data conversion', function () {
  this.timeout(60_000);
  let testEnv: TestWorkflowEnvironment;

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
  });

  after(async () => {
    await testEnv?.teardown();
  });

  // Runs a Workflow with a Worker and Client that both use the given payload converter
  async function runWithConverter<T>(
    folder: string,
    activities: object | undefined,
    fn: (client: Client, taskQueue: string) => Promise<T>,
  ): Promise<T> {
    const payloadConverterPath = require.resolve(`${dir}/${folder}/payload-converter`);
    const taskQueue = `data-conversion-${folder.replace('/', '-')}`;
    const worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      workflowsPath: require.resolve(`${dir}/${folder}/workflows`),
      activities,
      dataConverter: { payloadConverterPath },
    });
    const client = new Client({ connection: testEnv.connection, dataConverter: { payloadConverterPath } });
    return worker.runUntil(fn(client, taskQueue));
  }

  it('sends Dates, RegExps, Infinity, and Uint8Arrays with the EJSON payload converter', async () => {
    const user: User = {
      id: 'user-1',
      hp: Infinity,
      matcher: /.*Stormblessed/,
      token: Uint8Array.from([1, 2, 3]),
      createdAt: new Date(),
    };
    const result = await runWithConverter('ejson', undefined, (client, taskQueue) =>
      client.workflow.execute(ejsonExample, { args: [user], taskQueue, workflowId: `example-user-${user.id}` }),
    );
    assert.equal(result.success, true);
    assert.ok(result.at instanceof Date);
  });

  it('sends Protobuf messages with DefaultPayloadConverterWithProtobufs', async () => {
    const result = await runWithConverter('protobufs/src', protoActivities, (client, taskQueue) =>
      client.workflow.execute(protoExample, {
        args: [foo.bar.ProtoInput.create({ name: 'Proto', age: 2 })],
        taskQueue,
        workflowId: 'protobufs',
      }),
    );
    // The payload converter decodes the result back into a ProtoResult message
    assert.deepEqual(result.toJSON(), { sentence: 'Proto is 2 years old.' });
  });

  it('encodes Protobuf messages as binary with ProtobufBinaryPayloadConverter', () => {
    const message = ProtoResult.create({ sentence: 'binary' });
    const payload = binaryConverter.toPayload(message)!;
    assert.equal(encodingOf(payload), 'binary/protobuf');
    assert.equal(binaryConverter.fromPayload<ProtoResult>(payload).sentence, 'binary');
  });

  it('combines binary Protobufs with the default converters', () => {
    assert.equal(encodingOf(compositeConverter.toPayload(undefined)), 'binary/null');
    assert.equal(encodingOf(compositeConverter.toPayload(Uint8Array.from([1]))), 'binary/plain');
    assert.equal(encodingOf(compositeConverter.toPayload(ProtoResult.create({ sentence: 'x' }))), 'binary/protobuf');
    assert.equal(encodingOf(compositeConverter.toPayload({ plain: 'object' })), 'json/plain');
  });
});

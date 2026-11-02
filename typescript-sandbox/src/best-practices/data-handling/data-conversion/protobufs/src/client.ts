// @@@SNIPSTART typescript-data-conversion-protobuf-client
import { Client, Connection } from '@temporalio/client';
import { loadClientConnectConfig } from '@temporalio/envconfig';
import { nanoid } from 'nanoid';
import { foo, ProtoResult } from '../protos/root';
import { example } from './workflows';

async function run() {
  const config = loadClientConnectConfig();
  const connection = await Connection.connect(config.connectionOptions);
  const client = new Client({
    connection,
    dataConverter: { payloadConverterPath: require.resolve('./payload-converter') },
  });

  const handle = await client.workflow.start(example, {
    args: [foo.bar.ProtoInput.create({ name: 'Proto', age: 2 })],
    // can't do:
    // args: [new foo.bar.ProtoInput({ name: 'Proto', age: 2 })],
    taskQueue: 'protobufs',
    workflowId: 'my-business-id-' + nanoid(),
  });

  console.log(`Started workflow ${handle.workflowId}`);

  const result: ProtoResult = await handle.result();
  console.log(result.toJSON());
}
// @@@SNIPEND

run().catch((err) => {
  console.error(err);
  process.exit(1);
});

import { Client, Connection } from '@temporalio/client';
import { loadClientConnectConfig } from '@temporalio/envconfig';
import { nanoid } from 'nanoid';
import { getDataConverter } from './data-converter';
import { example } from './workflows';

async function run() {
  const config = loadClientConnectConfig();
  const connection = await Connection.connect(config.connectionOptions);
  // @@@SNIPSTART typescript-data-encryption-client
  const client = new Client({
    connection,
    dataConverter: await getDataConverter(),
  });

  const handle = await client.workflow.start(example, {
    args: ['Alice: Private message for Bob.'],
    taskQueue: 'encryption',
    workflowId: `my-business-id-${nanoid()}`,
  });

  console.log(`Started workflow ${handle.workflowId}`);
  console.log(await handle.result());
  // @@@SNIPEND
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});

// @@@SNIPSTART typescript-workflow-basics-client
import { Client, Connection } from '@temporalio/client';
import { example } from './parameters';

async function run() {
  const connection = await Connection.connect({ address: 'localhost:7233' });
  const client = new Client({ connection });

  const handle = await client.workflow.start(example, {
    args: [{ name: 'Temporal', born: 2019 }],
    taskQueue: 'your-queue',
    workflowId: 'business-meaningful-id',
  });
  console.log(await handle.result());
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});
// @@@SNIPEND

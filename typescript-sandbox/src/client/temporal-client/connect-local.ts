// @@@SNIPSTART typescript-client-connect-local
import { Connection, Client } from '@temporalio/client';

async function run() {
  const connection = await Connection.connect();

  // your code goes here

  const client = new Client({ connection });
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});
// @@@SNIPEND

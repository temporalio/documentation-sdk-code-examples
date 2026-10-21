import { Client, Connection, TemporalFailure, WorkflowFailedError } from '@temporalio/client';
import { loadClientConnectConfig } from '@temporalio/envconfig';
import { nanoid } from 'nanoid';
import { openAccount } from './workflows';
import { TASK_QUEUE } from './shared';

async function run() {
  const config = loadClientConnectConfig();
  const connection = await Connection.connect(config.connectionOptions);
  const client = new Client({ connection });

  const accountId = 'acct-' + nanoid(6);
  const handle = await client.workflow.start(openAccount, {
    taskQueue: TASK_QUEUE,
    args: [
      {
        accountId,
        address: '1 Main St',
        clientEmail: 'client@example.com',
        bankAccountId: 'bank-' + nanoid(6),
      },
    ],
    workflowId: 'open-account-' + accountId,
  });
  console.log(`Started Workflow ${handle.workflowId}`);

  try {
    console.log(await handle.result());
  } catch (err) {
    // This sample fails on purpose, so report the failure instead of crashing.
    if (err instanceof WorkflowFailedError) {
      // The cause chain is ActivityFailure -> ApplicationFailure; report the root cause.
      let cause: Error | undefined = err.cause;
      while (cause instanceof TemporalFailure && cause.cause) cause = cause.cause;
      console.log(`Workflow failed after compensating: ${cause?.message}`);
      return;
    }
    throw err;
  }
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});

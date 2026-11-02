import { Client, Connection } from '@temporalio/client';
import { loadClientConnectConfig } from '@temporalio/envconfig';

// Runs one of the Schedule operations in this folder against the Temporal Service from your environment config
export async function runWithClient(fn: (client: Client) => Promise<unknown>): Promise<void> {
  const config = loadClientConnectConfig();
  const connection = await Connection.connect(config.connectionOptions);
  try {
    await fn(new Client({ connection }));
  } catch (err) {
    console.error(err);
    process.exitCode = 1;
  } finally {
    await connection.close();
  }
}

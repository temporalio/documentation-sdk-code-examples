import { Client, Connection } from '@temporalio/client';

// Placeholder values: replace them with your Temporal Cloud endpoint, API key, and Namespace
export async function connectToCloud(): Promise<Client> {
  // @@@SNIPSTART typescript-client-connect-code
  const connection = await Connection.connect({
    address: '<endpoint>',
    tls: true,
    apiKey: '<APIKey>',
  });
  const client = new Client({
    connection,
    namespace: '<namespace_id>.<account_id>',
  });
  // @@@SNIPEND

  // @@@SNIPSTART typescript-client-set-api-key
  connection.setApiKey('<APIKey>');
  // @@@SNIPEND
  return client;
}

import { Connection, Client } from '@temporalio/client';
import { loadClientConnectConfig } from '@temporalio/envconfig';
import { nanoid } from 'nanoid';
import { myNexusService } from './api';
import { ENDPOINT_NAME } from './shared';

export async function executeEcho(client: Client): Promise<string> {
  // @@@SNIPSTART typescript-nexus-standalone-execute
  const nexusClient = client.nexus.createServiceClient({
    endpoint: ENDPOINT_NAME,
    service: myNexusService,
  });

  // Await the result of the operation immediately.
  const echoResult = await nexusClient.executeOperation(
    myNexusService.operations.echo,
    { message: 'hello' },
    {
      id: `echo-${nanoid()}`,
      scheduleToCloseTimeout: '10s',
    },
  );
  // @@@SNIPEND
  console.log(`Echo result: ${echoResult.message}`);
  return echoResult.message;
}

export async function startHello(client: Client): Promise<string> {
  const nexusClient = client.nexus.createServiceClient({
    endpoint: ENDPOINT_NAME,
    service: myNexusService,
  });

  // @@@SNIPSTART typescript-nexus-standalone-start
  // Start an operation and get a NexusOperationHandle
  const handle = await nexusClient.startOperation(
    myNexusService.operations.hello,
    { name: 'World' },
    {
      id: `hello-${nanoid()}`,
      scheduleToCloseTimeout: '10s',
    },
  );

  // Await the result
  const helloResult = await handle.result();
  console.log(helloResult.greeting);
  // @@@SNIPEND
  return helloResult.greeting;
}

export async function listOperations(client: Client): Promise<void> {
  // @@@SNIPSTART typescript-nexus-standalone-list
  const query = `Endpoint = "${ENDPOINT_NAME}"`;
  for await (const op of client.nexus.list({ query })) {
    console.log(`OperationId: ${op.operationId},`, `Operation: ${op.operation},`, `Status: ${op.status}`);
  }
  // @@@SNIPEND
}

export async function countOperations(client: Client): Promise<number> {
  // @@@SNIPSTART typescript-nexus-standalone-count
  const query = `Endpoint = "${ENDPOINT_NAME}"`;
  const count = await client.nexus.count(query);
  console.log(`Total Nexus operations: ${count.count}`);
  // @@@SNIPEND
  return count.count;
}

async function run() {
  // The client is configured via the SDK's environment configuration support, which reads
  // TEMPORAL_ADDRESS, TEMPORAL_NAMESPACE, etc. from the environment (and optionally a profile from
  // temporal.toml). The starter runs in the caller namespace.
  const config = loadClientConnectConfig();
  const connection = await Connection.connect(config.connectionOptions);
  try {
    const client = new Client({ connection, namespace: config.namespace ?? 'my-caller-namespace' });
    await executeEcho(client);
    await startHello(client);
    console.log('\nListing Nexus operations:');
    await listOperations(client);
    await countOperations(client);
  } finally {
    await connection.close();
  }
}

if (require.main === module) {
  run().catch((err) => {
    console.error(err);
    process.exit(1);
  });
}

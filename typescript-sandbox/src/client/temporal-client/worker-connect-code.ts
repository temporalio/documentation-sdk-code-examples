import { readFileSync } from 'fs';
import { NativeConnection, Worker } from '@temporalio/worker';
import * as activities from './activities';

// Placeholder values: replace them with your Temporal Cloud endpoint, API key, Namespace, and certificate files
export async function createCloudWorker(): Promise<Worker> {
  // @@@SNIPSTART typescript-client-worker-connect-code
  const connection = await NativeConnection.connect({
    address: '<endpoint>',
    tls: true,
    apiKey: '<APIKey>',
  });
  const worker = await Worker.create({
    connection,
    namespace: '<namespace_id>.<account_id>',
    taskQueue: 'your-task-queue',
    workflowsPath: require.resolve('./workflows'),
    activities,
  });
  // @@@SNIPEND
  return worker;
}

export async function rotateCertificate(worker: Worker): Promise<void> {
  // @@@SNIPSTART typescript-client-worker-rotate-certificate
  const newConnection = await NativeConnection.connect({
    address: '<endpoint>',
    tls: {
      clientCertPair: {
        crt: readFileSync('client-cert-new.pem'),
        key: readFileSync('client-key-new.pem'),
      },
    },
  });

  worker.connection = newConnection;
  // @@@SNIPEND
}

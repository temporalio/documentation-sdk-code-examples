import { NativeConnection } from '@temporalio/worker';

// @@@SNIPSTART typescript-activity-basics-dependency-injection-worker
import { Worker } from '@temporalio/worker';
import { createActivities } from './di-activities';

export async function createDependencyInjectionWorker(connection: NativeConnection): Promise<Worker> {
  // Mock DB connection initialization in Worker
  const db = {
    async get(_key: string) {
      return 'Temporal';
    },
  };

  return await Worker.create({
    connection,
    taskQueue: 'dependency-injection',
    workflowsPath: require.resolve('./workflows'),
    activities: createActivities(db),
  });
}
// @@@SNIPEND

async function run() {
  const connection = await NativeConnection.connect({ address: 'localhost:7233' });
  try {
    const worker = await createDependencyInjectionWorker(connection);
    await worker.run();
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

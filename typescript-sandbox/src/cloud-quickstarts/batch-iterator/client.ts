import { Client, Connection } from "@temporalio/client";
import { loadClientConnectConfig } from "@temporalio/envconfig";
import { PAGE_SIZE, TASK_QUEUE } from "./shared";
import { iteratorBatchWorkflow } from "./workflows";

async function run() {
  // Same environment-configuration lookup the Worker uses: TEMPORAL_PROFILE selects the
  // profile, the TOML file supplies address/namespace/credentials, and TEMPORAL_* env
  // vars can override individual fields.
  const config = loadClientConnectConfig();
  const address = config.connectionOptions.address ?? "localhost:7233";
  const namespace = config.namespace ?? "default";
  const connection = await Connection.connect({
    ...config.connectionOptions,
    address,
  });
  try {
    const client = new Client({ connection, namespace });

    const handle = await client.workflow.start(iteratorBatchWorkflow, {
      args: [PAGE_SIZE, 0],
      workflowId: `batch-iterator-${Date.now()}`,
      taskQueue: TASK_QUEUE,
    });
    console.log(
      `Started batch Workflow ${handle.workflowId} against ${address} (namespace: ${namespace})...`,
    );

    // result() follows the continue-as-new chain and returns the final run's result.
    const processed = await handle.result();
    console.log(`Batch complete: processed ${processed} records`);
  } finally {
    await connection.close();
  }
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});

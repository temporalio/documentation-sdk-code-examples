import { loadClientConnectConfig } from "@temporalio/envconfig";
import { NativeConnection, Worker } from "@temporalio/worker";
import * as activities from "./activities";
import { TASK_QUEUE } from "./shared";

async function run() {
  // loadClientConnectConfig reads the TEMPORAL_PROFILE env var (default "default"),
  // loads that profile from the TOML config file (default OS config dir, or
  // TEMPORAL_CONFIG_FILE), and applies any TEMPORAL_* overrides. No connection details
  // are hardcoded, so the same Worker runs against a local dev server or Temporal Cloud.
  const config = loadClientConnectConfig();
  const address = config.connectionOptions.address ?? "localhost:7233";
  const namespace = config.namespace ?? "default";
  const connection = await NativeConnection.connect({
    ...config.connectionOptions,
    address,
  });
  try {
    const worker = await Worker.create({
      connection,
      namespace,
      taskQueue: TASK_QUEUE,
      workflowsPath: require.resolve("./workflows"),
      activities,
    });
    console.log(
      `Worker connected to ${address} (namespace: ${namespace}), polling task queue '${TASK_QUEUE}'`,
    );
    await worker.run();
  } finally {
    await connection.close();
  }
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});

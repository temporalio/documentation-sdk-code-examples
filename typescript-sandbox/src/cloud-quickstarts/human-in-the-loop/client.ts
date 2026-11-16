import {
  Client,
  Connection,
  WorkflowExecutionAlreadyStartedError,
} from "@temporalio/client";
import { loadClientConnectConfig } from "@temporalio/envconfig";
import { TASK_QUEUE, WORKFLOW_ID } from "./shared";
import { humanInTheLoopWorkflow } from "./workflows";

const DEFAULT_REQUEST = "Delete all test data from the production database";

async function run() {
  const userRequest = process.argv[2] || DEFAULT_REQUEST;

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

    let handle;
    try {
      handle = await client.workflow.start(humanInTheLoopWorkflow, {
        args: [{ user_request: userRequest, approval_timeout_seconds: 300 }],
        workflowId: WORKFLOW_ID,
        taskQueue: TASK_QUEUE,
      });
    } catch (err) {
      if (err instanceof WorkflowExecutionAlreadyStartedError) {
        console.log(
          `Workflow ${WORKFLOW_ID} is already running. Approve or reject it, wait for it ` +
            `to time out, or terminate it:\n  temporal workflow terminate --workflow-id ${WORKFLOW_ID}`,
        );
        process.exitCode = 1;
        return;
      }
      throw err;
    }

    console.log(
      `Started Workflow ${handle.workflowId} against ${address} (namespace: ${namespace})...`,
    );
    console.log(`Request: ${userRequest}`);
    console.log(
      "If the action is risky, the Worker prints how to approve or reject it.",
    );

    console.log(`Result: ${await handle.result()}`);
  } finally {
    await connection.close();
  }
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});

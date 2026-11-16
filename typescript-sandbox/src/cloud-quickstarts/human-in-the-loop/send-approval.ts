import { Client, Connection } from "@temporalio/client";
import { loadClientConnectConfig } from "@temporalio/envconfig";
import { WORKFLOW_ID } from "./shared";
import { approvalDecisionSignal } from "./workflows";

const USAGE =
  "Usage: npm run human-in-the-loop.send-approval -- <approve|reject> [notes]";

async function run() {
  const decision = process.argv[2]?.toLowerCase();
  if (decision !== "approve" && decision !== "reject") {
    console.log(USAGE);
    process.exitCode = 1;
    return;
  }
  const notes = process.argv[3];

  // Same environment-configuration lookup the Worker and client use.
  const config = loadClientConnectConfig();
  const connection = await Connection.connect({
    ...config.connectionOptions,
    address: config.connectionOptions.address ?? "localhost:7233",
  });
  try {
    const client = new Client({
      connection,
      namespace: config.namespace ?? "default",
    });

    // Deliver the reviewer's decision to the waiting Workflow as a Signal. With no
    // request_id, it applies to whichever request is pending.
    await client.workflow
      .getHandle(WORKFLOW_ID)
      .signal(approvalDecisionSignal, {
        approved: decision === "approve",
        reviewer_notes: notes,
        decided_at: new Date().toISOString(),
      });

    console.log(`Sent ${decision} decision to Workflow ${WORKFLOW_ID}`);
  } finally {
    await connection.close();
  }
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});

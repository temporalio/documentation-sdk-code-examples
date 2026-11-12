import {
  continueAsNew,
  executeChild,
  log,
  proxyActivities,
  sleep,
  workflowInfo,
} from "@temporalio/workflow";
import type * as activities from "./activities";
import type { SingleRecord } from "./types";

const { getRecords } = proxyActivities<typeof activities>({
  startToCloseTimeout: "5 seconds",
});

/**
 * Implements the iterator pattern.
 *
 * A single Workflow run processes one page of records in parallel, starting one
 * recordProcessorWorkflow child per record. After every child completes, the Workflow
 * continues as new with the next offset, so each run's Event History stays small no
 * matter how large the dataset is.
 *
 * @param pageSize the number of records to process in a single Workflow run.
 * @param offset the offset of the first record to process. 0 to start the batch.
 * @returns total number of processed records.
 */
export async function iteratorBatchWorkflow(
  pageSize: number,
  offset: number,
): Promise<number> {
  const records = await getRecords(pageSize, offset);

  // No more records in the dataset: the batch is done.
  if (records.length === 0) {
    return offset;
  }

  // Start one child per record with a human-friendly ID, e.g. "batch-iterator-123/7",
  // then wait for every child in this page. Error handling is skipped for brevity.
  const { workflowId } = workflowInfo();
  await Promise.all(
    records.map((record) =>
      executeChild(recordProcessorWorkflow, {
        args: [record],
        workflowId: `${workflowId}/${record.id}`,
      }),
    ),
  );

  return await continueAsNew<typeof iteratorBatchWorkflow>(
    pageSize,
    offset + records.length,
  );
}

/** Child Workflow that processes a single record. */
export async function recordProcessorWorkflow(
  record: SingleRecord,
): Promise<void> {
  // Simulate 1-3 seconds of work. Kept short so the whole batch finishes in seconds.
  // Math.random() is deterministic on replay inside the Workflow sandbox.
  await sleep(`${1 + Math.floor(Math.random() * 3)} seconds`);
  log.info("Processed record", { id: record.id });
}

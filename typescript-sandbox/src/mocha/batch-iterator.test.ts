import { TestWorkflowEnvironment } from "@temporalio/testing";
import { after, before, describe, it } from "mocha";
import { Worker } from "@temporalio/worker";
import assert from "assert";
import * as activities from "../cloud-quickstarts/batch-iterator/activities";
import { PAGE_COUNT } from "../cloud-quickstarts/batch-iterator/activities";
import { PAGE_SIZE } from "../cloud-quickstarts/batch-iterator/shared";
import { iteratorBatchWorkflow } from "../cloud-quickstarts/batch-iterator/workflows";

describe("Batch iterator quickstart", function () {
  this.timeout(60_000);
  let testEnv: TestWorkflowEnvironment;

  before(async () => {
    // Time skipping makes the children's 1-3 second sleeps instant.
    testEnv = await TestWorkflowEnvironment.createTimeSkipping();
  });

  after(async () => {
    await testEnv?.teardown();
  });

  it("processes every record across continue-as-new runs", async () => {
    const taskQueue = "batch-iterator";
    const workflowId = "batch-iterator-test";
    const total = PAGE_SIZE * PAGE_COUNT;
    const worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      workflowsPath:
        require.resolve("../cloud-quickstarts/batch-iterator/workflows"),
      activities,
    });

    await worker.runUntil(async () => {
      const handle = await testEnv.client.workflow.start(
        iteratorBatchWorkflow,
        {
          args: [PAGE_SIZE, 0],
          workflowId,
          taskQueue,
        },
      );
      assert.equal(await handle.result(), total);

      const firstRun = testEnv.client.workflow.getHandle(
        workflowId,
        handle.firstExecutionRunId,
      );
      assert.equal((await firstRun.describe()).status.name, "CONTINUED_AS_NEW");

      // Every record got its own completed child Workflow.
      for (let id = 0; id < total; id++) {
        const child = await testEnv.client.workflow
          .getHandle(`${workflowId}/${id}`)
          .describe();
        assert.equal(child.status.name, "COMPLETED");
      }
    });
  });
});

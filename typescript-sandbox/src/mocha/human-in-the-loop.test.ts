import {
  MockActivityEnvironment,
  TestWorkflowEnvironment,
} from "@temporalio/testing";
import { Worker } from "@temporalio/worker";
import assert from "assert";
import { after, before, describe, it } from "mocha";
import { proposeAction } from "../cloud-quickstarts/human-in-the-loop/activities";
import type {
  ApprovalRequest,
  ProposedAction,
} from "../cloud-quickstarts/human-in-the-loop/shared";
import {
  approvalDecisionSignal,
  humanInTheLoopWorkflow,
} from "../cloud-quickstarts/human-in-the-loop/workflows";

const SAFE: ProposedAction = {
  action_type: "list_files",
  description: "List directory contents",
  reasoning: "Read-only operation",
  risky_action: false,
};
const RISKY: ProposedAction = {
  action_type: "delete_data",
  description: "Delete production data",
  reasoning: "User asked for deletion",
  risky_action: true,
};

describe("Human-in-the-loop quickstart", function () {
  this.timeout(60_000);
  let testEnv: TestWorkflowEnvironment;

  before(async () => {
    // Time skipping makes the approval timeout fire instantly.
    testEnv = await TestWorkflowEnvironment.createTimeSkipping();
  });

  after(async () => {
    await testEnv?.teardown();
  });

  /**
   * Runs the Workflow with mocked Activities, so the tests need no API key. If
   * `decide` is set, sends it as the reviewer's decision once approval is requested.
   */
  async function runWorkflow(
    action: ProposedAction,
    timeoutSeconds: number,
    decide?: { approved: boolean; notes: string },
    withRequestId = true,
  ): Promise<string> {
    let resolveRequest!: (request: ApprovalRequest) => void;
    const requested = new Promise<ApprovalRequest>((resolve) => {
      resolveRequest = resolve;
    });

    const taskQueue = `human-in-the-loop-${Math.random()}`;
    const worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      workflowsPath:
        require.resolve("../cloud-quickstarts/human-in-the-loop/workflows"),
      activities: {
        proposeAction: async () => action,
        notifyApprovalNeeded: async (request: ApprovalRequest) =>
          resolveRequest(request),
        executeAction: async (proposed: ProposedAction) =>
          `Executed: ${proposed.action_type}`,
      },
    });

    return await worker.runUntil(async () => {
      const handle = await testEnv.client.workflow.start(
        humanInTheLoopWorkflow,
        {
          args: [
            { user_request: "test", approval_timeout_seconds: timeoutSeconds },
          ],
          workflowId: `human-in-the-loop-${Math.random()}`,
          taskQueue,
          workflowRunTimeout: "30 seconds",
        },
      );
      if (decide) {
        const request = await requested;
        await handle.signal(approvalDecisionSignal, {
          approved: decide.approved,
          request_id: withRequestId ? request.request_id : undefined,
          reviewer_notes: decide.notes,
          decided_at: "2026-01-01T00:00:00Z",
        });
      }
      return await handle.result();
    });
  }

  it("auto-approves a safe action", async () => {
    assert.equal(
      await runWorkflow(SAFE, 30),
      "Action completed successfully (auto-approved): Executed: list_files",
    );
  });

  it("runs a risky action when approved", async () => {
    assert.equal(
      await runWorkflow(RISKY, 300, { approved: true, notes: "Looks good" }),
      "Action completed successfully: Executed: delete_data",
    );
  });

  it("applies a decision without a request_id to the pending request", async () => {
    assert.equal(
      await runWorkflow(
        RISKY,
        300,
        { approved: true, notes: "Looks good" },
        false,
      ),
      "Action completed successfully: Executed: delete_data",
    );
  });

  it("rejects a risky action", async () => {
    assert.equal(
      await runWorkflow(RISKY, 300, { approved: false, notes: "Too risky" }),
      "Action rejected. Reviewer notes: Too risky",
    );
  });

  it("times out a risky action without a decision", async () => {
    assert.match(await runWorkflow(RISKY, 1), /timed out after 1 seconds/);
  });

  it("returns a risky mock action without an API key", async () => {
    const saved = process.env.OPENAI_API_KEY;
    delete process.env.OPENAI_API_KEY;
    try {
      const action: ProposedAction = await new MockActivityEnvironment().run(
        proposeAction,
        {
          model: "gpt-4o-mini",
          instructions: "",
          input: "test",
        },
      );
      assert.equal(action.risky_action, true);
      assert.equal(action.action_type, "delete_test_data");
    } finally {
      if (saved !== undefined) process.env.OPENAI_API_KEY = saved;
    }
  });
});

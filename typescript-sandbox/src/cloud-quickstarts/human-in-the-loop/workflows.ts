/**
 * An AI agent that asks a human to approve risky actions.
 *
 * An LLM proposes an action for the user's request. Safe actions run right away. Risky
 * actions wait for an approval decision delivered by a Signal, with a durable timeout
 * in case no one responds.
 */
import {
  condition,
  defineSignal,
  log,
  proxyActivities,
  setHandler,
  uuid4,
} from "@temporalio/workflow";
import type * as activities from "./activities";
import type { ApprovalDecision, ProposedAction, WorkflowInput } from "./shared";

/** Delivers a reviewer's approve or reject decision. */
export const approvalDecisionSignal =
  defineSignal<[ApprovalDecision]>("approval_decision");

/** Tells the LLM how to analyze a request and assess its risk. */
export const SYSTEM_INSTRUCTIONS = `
You are an AI assistant that analyzes user requests and proposes actions.
For each request, you should:
1. Determine what action needs to be taken
2. Provide a clear description of the action
3. Explain your reasoning for why this action addresses the request
4. Assess whether this is a risky action that requires human approval

Consider an action risky if it:
- Could cause data loss or corruption
- Could affect production systems or critical infrastructure
- Could have financial or legal implications
- Could impact user experience or system availability
- Involves deleting, modifying, or overwriting important data
- Could expose sensitive information or security vulnerabilities

Be thorough and clear in your analysis.

Respond with a JSON object in this structure:

{
  "action_type": "A short name for the action (e.g., \\"delete_test_data\\")",
  "description": "A clear description of what the action will do",
  "reasoning": "Your explanation for why this action addresses the request",
  "risky_action": true or false indicating whether this action is considered risky
}
`;

const { proposeAction } = proxyActivities<typeof activities>({
  startToCloseTimeout: "30 seconds",
});
const { notifyApprovalNeeded } = proxyActivities<typeof activities>({
  startToCloseTimeout: "10 seconds",
});
const { executeAction } = proxyActivities<typeof activities>({
  startToCloseTimeout: "60 seconds",
});

export async function humanInTheLoopWorkflow(
  input: WorkflowInput,
): Promise<string> {
  // The Signal handler writes these; the wait condition reads them.
  let currentDecision: ApprovalDecision | undefined;
  let pendingRequestId: string | undefined;

  setHandler(approvalDecisionSignal, (decision) => {
    if (pendingRequestId === undefined) {
      log.warn("Ignoring decision: no approval request is pending");
    }
    // A decision without a request_id applies to the pending request. One with a
    // request_id must match it, so a stale decision is ignored.
    else if (
      decision.request_id == null ||
      decision.request_id === pendingRequestId
    ) {
      currentDecision = decision;
    } else {
      log.warn(
        `Ignoring decision for request ${decision.request_id}; expected ${pendingRequestId}`,
      );
    }
  });

  /** Notifies a reviewer and waits for a decision. Returns undefined on timeout. */
  async function requestApproval(
    action: ProposedAction,
  ): Promise<ApprovalDecision | undefined> {
    currentDecision = undefined;
    // uuid4() is deterministic in Workflow code, so it's safe on replay.
    pendingRequestId = uuid4();

    await notifyApprovalNeeded({
      request_id: pendingRequestId,
      proposed_action: action,
      context: input.user_request,
      // Date is deterministic inside the Workflow sandbox.
      requested_at: new Date().toISOString(),
    });

    // A durable timer: the Workflow uses no compute while it waits, and the timeout
    // survives Worker restarts.
    const decided = await condition(
      () => currentDecision !== undefined,
      `${input.approval_timeout_seconds} seconds`,
    );
    return decided ? currentDecision : undefined;
  }

  const proposed = await proposeAction({
    model: "gpt-4o-mini",
    instructions: SYSTEM_INSTRUCTIONS,
    input: input.user_request,
  });
  log.info(
    `AI proposed action: ${proposed.action_type} (risky: ${proposed.risky_action})`,
  );

  if (!proposed.risky_action) {
    return `Action completed successfully (auto-approved): ${await executeAction(proposed)}`;
  }

  const decision = await requestApproval(proposed);
  if (decision === undefined) {
    return `Action cancelled: approval request timed out after ${input.approval_timeout_seconds} seconds`;
  }
  if (!decision.approved) {
    return `Action rejected. Reviewer notes: ${decision.reviewer_notes ?? "None provided"}`;
  }
  return `Action completed successfully: ${await executeAction(proposed)}`;
}

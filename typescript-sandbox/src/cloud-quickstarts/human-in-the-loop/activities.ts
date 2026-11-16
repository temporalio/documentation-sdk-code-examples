import { Context, log } from "@temporalio/activity";
import OpenAI from "openai";
import type { ApprovalRequest, ProposedAction } from "./shared";

/** Input for the proposeAction Activity. */
export interface OpenAIResponsesRequest {
  model: string;
  instructions: string;
  input: string;
}

// JSON schema for ProposedAction. Strict structured outputs require every property to
// be listed as required and additionalProperties to be false.
const PROPOSED_ACTION_SCHEMA = {
  type: "object",
  properties: {
    action_type: { type: "string" },
    description: { type: "string" },
    reasoning: { type: "string" },
    risky_action: { type: "boolean" },
  },
  required: ["action_type", "description", "reasoning", "risky_action"],
  additionalProperties: false,
};

/**
 * A fixed, risky action used when no OpenAI API key is set, so the sample runs end to
 * end (including the approval step) without an LLM.
 */
export function mockProposedAction(userRequest: string): ProposedAction {
  return {
    action_type: "delete_test_data",
    description: `Mock action for the request: ${userRequest}`,
    reasoning:
      "OPENAI_API_KEY is not set, so this action is mocked instead of LLM-generated.",
    risky_action: true,
  };
}

function isProposedAction(value: unknown): value is ProposedAction {
  const v = value as Partial<ProposedAction> | null;
  return (
    typeof v?.action_type === "string" &&
    typeof v.description === "string" &&
    typeof v.reasoning === "string" &&
    typeof v.risky_action === "boolean"
  );
}

/**
 * Asks the LLM for a proposed action. If OPENAI_API_KEY isn't set, returns a mocked
 * proposed action instead.
 */
export async function proposeAction(
  request: OpenAIResponsesRequest,
): Promise<ProposedAction> {
  if (!process.env.OPENAI_API_KEY) {
    log.warn(
      "OPENAI_API_KEY is not set; returning a mock proposed action instead of calling OpenAI.",
    );
    return mockProposedAction(request.input);
  }

  // Let Temporal own retries: disable the OpenAI client's built-in retry logic.
  const client = new OpenAI({ maxRetries: 0, timeout: 30_000 });

  // Structured outputs: the API constrains the response to ProposedAction's JSON
  // schema.
  const response = await client.responses.create(
    {
      model: request.model,
      instructions: request.instructions,
      input: request.input,
      text: {
        format: {
          type: "json_schema",
          name: "proposed_action",
          schema: PROPOSED_ACTION_SCHEMA,
          strict: true,
        },
      },
    },
    { signal: Context.current().cancellationSignal },
  );

  // Parse here, not in the Workflow: if the output is unusable, this Activity attempt
  // fails and Temporal retries the LLM call. Throwing in Workflow code would instead
  // fail the Workflow Task and replay the same bad result.
  let parsed: unknown;
  try {
    parsed = JSON.parse(response.output_text);
  } catch {
    parsed = undefined;
  }
  if (!isProposedAction(parsed)) {
    throw new Error(
      `LLM returned no parsable ProposedAction: ${JSON.stringify(response.output_text)}`,
    );
  }
  return parsed;
}

/**
 * Tells a human that an action is waiting for approval.
 *
 * This sample prints to the Worker's terminal. A real system might send an email, post
 * to Slack, or update an approval queue UI instead.
 */
export async function notifyApprovalNeeded(
  request: ApprovalRequest,
): Promise<void> {
  const workflowId = Context.current().info.workflowExecution?.workflowId;
  log.info(`Approval needed for request: ${request.request_id}`);

  const line = "=".repeat(60);
  console.log(`
${line}
APPROVAL REQUIRED
${line}
Workflow ID: ${workflowId}
Request ID:  ${request.request_id}
Action:      ${request.proposed_action.action_type}
Description: ${request.proposed_action.description}
Reasoning:   ${request.proposed_action.reasoning}

To approve or reject, run:
  npm run human-in-the-loop.send-approval -- approve "Looks good"
  npm run human-in-the-loop.send-approval -- reject "Too risky"

Or with the Temporal CLI:
  temporal workflow signal --workflow-id ${workflowId} --name approval_decision --input '{"approved": true}'
${line}
`);
}

/**
 * Carries out the approved action.
 *
 * This sample only logs the action. A real system would call an external API, change a
 * database, or invoke a tool the LLM selected.
 */
export async function executeAction(action: ProposedAction): Promise<string> {
  log.info(`Executing action: ${action.action_type}`);
  return `Successfully executed: ${action.action_type}`;
}

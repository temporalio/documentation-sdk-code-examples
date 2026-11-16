import os
from dataclasses import dataclass

from openai import AsyncOpenAI
from temporalio import activity

from shared import ApprovalRequest, ProposedAction


@dataclass
class OpenAIResponsesRequest:
    model: str
    instructions: str
    input: str


def mock_proposed_action(user_request: str) -> ProposedAction:
    """A fixed, risky action used when no OpenAI API key is set, so the sample
    runs end to end (including the approval step) without an LLM."""
    return ProposedAction(
        action_type="delete_test_data",
        description=f"Mock action for the request: {user_request}",
        reasoning="OPENAI_API_KEY is not set, so this action is mocked instead of LLM-generated.",
        risky_action=True,
    )


@activity.defn
async def create(request: OpenAIResponsesRequest) -> str:
    """Call the OpenAI Responses API and return the text output.

    If OPENAI_API_KEY isn't set, return a mocked proposed action instead.
    """
    if not os.environ.get("OPENAI_API_KEY"):
        activity.logger.warning(
            "OPENAI_API_KEY is not set; returning a mock proposed action instead of calling OpenAI."
        )
        return mock_proposed_action(request.input).model_dump_json()

    # Let Temporal own retries: disable the OpenAI client's built-in retry logic.
    client = AsyncOpenAI(max_retries=0)

    resp = await client.responses.create(
        model=request.model,
        instructions=request.instructions,
        input=request.input,
        timeout=30,
    )

    return resp.output_text


@activity.defn
async def notify_approval_needed(request: ApprovalRequest) -> None:
    """Tell a human that an action is waiting for approval.

    This sample prints to the Worker's terminal. A real system might send an
    email, post to Slack, or update an approval queue UI instead.
    """
    workflow_id = activity.info().workflow_id

    activity.logger.info(f"Approval needed for request: {request.request_id}")

    print(f"\n{'=' * 60}")
    print("APPROVAL REQUIRED")
    print(f"{'=' * 60}")
    print(f"Workflow ID: {workflow_id}")
    print(f"Request ID:  {request.request_id}")
    print(f"Action:      {request.proposed_action.action_type}")
    print(f"Description: {request.proposed_action.description}")
    print(f"Reasoning:   {request.proposed_action.reasoning}")
    print("\nTo approve or reject, run:")
    print("  uv run send_approval.py approve 'Looks good'")
    print("  uv run send_approval.py reject 'Too risky'")
    print("\nOr with the Temporal CLI:")
    print(
        f"  temporal workflow signal --workflow-id {workflow_id} "
        "--name approval_decision --input '{\"approved\": true}'"
    )
    print(f"{'=' * 60}\n", flush=True)


@activity.defn
async def execute_action(action: ProposedAction) -> str:
    """Carry out the approved action.

    This sample only logs the action. A real system would call an external API,
    change a database, or invoke a tool the LLM selected.
    """
    activity.logger.info(f"Executing action: {action.action_type}")
    return f"Successfully executed: {action.action_type}"

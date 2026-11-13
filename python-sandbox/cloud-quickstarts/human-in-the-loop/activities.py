from dataclasses import dataclass

from openai import AsyncOpenAI
from temporalio import activity

from shared import ApprovalRequest, ProposedAction


@dataclass
class OpenAIResponsesRequest:
    model: str
    instructions: str
    input: str


@activity.defn
async def create(request: OpenAIResponsesRequest) -> str:
    """Call the OpenAI Responses API and return the text output."""
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
    print(f"  uv run send_approval.py {workflow_id} {request.request_id} approve 'Looks good'")
    print(f"  uv run send_approval.py {workflow_id} {request.request_id} reject 'Too risky'")
    print(f"{'=' * 60}\n", flush=True)


@activity.defn
async def execute_action(action: ProposedAction) -> str:
    """Carry out the approved action.

    This sample only logs the action. A real system would call an external API,
    change a database, or invoke a tool the LLM selected.
    """
    activity.logger.info(f"Executing action: {action.action_type}")
    return f"Successfully executed: {action.action_type}"

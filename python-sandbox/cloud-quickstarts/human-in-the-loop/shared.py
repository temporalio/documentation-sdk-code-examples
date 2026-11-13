import os

from pydantic import BaseModel

# The Task Queue the Worker polls and the client targets. Keep this in sync
# between the Worker and the client.
TASK_QUEUE = os.getenv("TEMPORAL_TASK_QUEUE", "quickstart-human-in-the-loop")


class WorkflowInput(BaseModel):
    """Input for HumanInTheLoopWorkflow."""

    user_request: str
    approval_timeout_seconds: int = 300


class ProposedAction(BaseModel):
    """Action the LLM proposes in response to the user request."""

    action_type: str
    description: str
    reasoning: str
    risky_action: bool


class ApprovalRequest(BaseModel):
    """Request sent to a human reviewer."""

    request_id: str
    proposed_action: ProposedAction
    context: str
    requested_at: str


class ApprovalDecision(BaseModel):
    """Decision a human reviewer sends back with a Signal."""

    request_id: str
    approved: bool
    reviewer_notes: str | None = None
    decided_at: str

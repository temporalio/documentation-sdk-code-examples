import asyncio
import uuid
from datetime import timedelta

from temporalio import activity
from temporalio.contrib.pydantic import pydantic_data_converter
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from activities import OpenAIResponsesRequest
from shared import ApprovalDecision, ApprovalRequest, ProposedAction, WorkflowInput
from workflow import HumanInTheLoopWorkflow

SAFE = ProposedAction(
    action_type="list_files",
    description="List directory contents",
    reasoning="Read-only operation",
    risky_action=False,
)
RISKY = ProposedAction(
    action_type="delete_data",
    description="Delete production data",
    reasoning="User asked for deletion",
    risky_action=True,
)


def mock_create(action: ProposedAction):
    """Stand-in for the OpenAI Activity, so the tests need no API key."""

    @activity.defn(name="create")
    async def create(request: OpenAIResponsesRequest) -> str:
        return action.model_dump_json()

    return create


@activity.defn(name="execute_action")
async def mock_execute(action: ProposedAction) -> str:
    return f"Executed: {action.action_type}"


async def run_workflow(action: ProposedAction, timeout_seconds: int, decide=None) -> str:
    """Run the Workflow with mocked Activities. If `decide` is set, send it as
    the reviewer's (approved, notes) once approval is requested."""
    requests: asyncio.Queue[ApprovalRequest] = asyncio.Queue()

    @activity.defn(name="notify_approval_needed")
    async def mock_notify(request: ApprovalRequest) -> None:
        await requests.put(request)

    task_queue = str(uuid.uuid4())
    # Time skipping makes the approval timeout fire instantly.
    async with await WorkflowEnvironment.start_time_skipping(
        data_converter=pydantic_data_converter
    ) as env:
        async with Worker(
            env.client,
            task_queue=task_queue,
            workflows=[HumanInTheLoopWorkflow],
            activities=[mock_create(action), mock_notify, mock_execute],
        ):
            handle = await env.client.start_workflow(
                HumanInTheLoopWorkflow.run,
                WorkflowInput(user_request="test", approval_timeout_seconds=timeout_seconds),
                id=f"human-in-the-loop-{uuid.uuid4()}",
                task_queue=task_queue,
                run_timeout=timedelta(seconds=30),
            )
            if decide is not None:
                request = await asyncio.wait_for(requests.get(), timeout=10)
                approved, notes = decide
                await handle.signal(
                    HumanInTheLoopWorkflow.approval_decision,
                    ApprovalDecision(
                        request_id=request.request_id,
                        approved=approved,
                        reviewer_notes=notes,
                        decided_at="2026-01-01T00:00:00Z",
                    ),
                )
            return await handle.result()


async def test_safe_action_is_auto_approved():
    result = await run_workflow(SAFE, timeout_seconds=30)
    assert result == "Action completed successfully (auto-approved): Executed: list_files"


async def test_risky_action_runs_when_approved():
    result = await run_workflow(RISKY, timeout_seconds=300, decide=(True, "Looks good"))
    assert result == "Action completed successfully: Executed: delete_data"


async def test_risky_action_is_rejected():
    result = await run_workflow(RISKY, timeout_seconds=300, decide=(False, "Too risky"))
    assert result == "Action rejected. Reviewer notes: Too risky"


async def test_risky_action_times_out_without_a_decision():
    result = await run_workflow(RISKY, timeout_seconds=1)
    assert "timed out after 1 seconds" in result

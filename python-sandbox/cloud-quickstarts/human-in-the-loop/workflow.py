"""An AI agent that asks a human to approve risky actions.

An LLM proposes an action for the user's request. Safe actions run right away.
Risky actions wait for an approval decision delivered by a Signal, with a
durable timeout in case no one responds.
"""

import asyncio
from datetime import timedelta
from typing import Optional

from temporalio import workflow

with workflow.unsafe.imports_passed_through():
    from activities import (
        OpenAIResponsesRequest,
        create,
        execute_action,
        notify_approval_needed,
    )
    from shared import (
        ApprovalDecision,
        ApprovalRequest,
        ProposedAction,
        WorkflowInput,
    )

SYSTEM_INSTRUCTIONS = """
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

Respond with a JSON string in this structure:

{
  "action_type": "A short name for the action (e.g., \\"delete_test_data\\")",
  "description": "A clear description of what the action will do",
  "reasoning": "Your explanation for why this action addresses the request",
  "risky_action": true or false indicating whether this action is considered risky
}
"""


@workflow.defn
class HumanInTheLoopWorkflow:
    def __init__(self) -> None:
        # The Signal handler writes these; the wait condition reads them.
        self.current_decision: Optional[ApprovalDecision] = None
        self.pending_request_id: Optional[str] = None

    @workflow.run
    async def run(self, input: WorkflowInput) -> str:
        proposed_action = await self._propose_action(input.user_request)
        workflow.logger.info(
            f"AI proposed action: {proposed_action.action_type} "
            f"(risky: {proposed_action.risky_action})"
        )

        if not proposed_action.risky_action:
            result = await self._execute(proposed_action)
            return f"Action completed successfully (auto-approved): {result}"

        outcome = await self._request_approval(
            proposed_action,
            input.user_request,
            timeout_seconds=input.approval_timeout_seconds,
        )

        if outcome == "approved":
            result = await self._execute(proposed_action)
            return f"Action completed successfully: {result}"
        if outcome == "rejected":
            notes = self.current_decision.reviewer_notes or "None provided"
            return f"Action rejected. Reviewer notes: {notes}"
        return (
            "Action cancelled: approval request timed out after "
            f"{input.approval_timeout_seconds} seconds"
        )

    @workflow.signal
    async def approval_decision(self, decision: ApprovalDecision) -> None:
        # Ignore decisions meant for an earlier or unknown request.
        if decision.request_id == self.pending_request_id:
            self.current_decision = decision
        else:
            workflow.logger.warning(
                f"Ignoring decision for request {decision.request_id}; "
                f"expected {self.pending_request_id}"
            )

    async def _propose_action(self, user_request: str) -> ProposedAction:
        result = await workflow.execute_activity(
            create,
            OpenAIResponsesRequest(
                model="gpt-4o-mini",
                instructions=SYSTEM_INSTRUCTIONS,
                input=user_request,
            ),
            start_to_close_timeout=timedelta(seconds=30),
        )
        return ProposedAction.model_validate_json(result)

    async def _execute(self, action: ProposedAction) -> str:
        return await workflow.execute_activity(
            execute_action,
            action,
            start_to_close_timeout=timedelta(seconds=60),
        )

    async def _request_approval(
        self, action: ProposedAction, context: str, timeout_seconds: int
    ) -> str:
        """Notify a reviewer and wait for a decision. Returns "approved",
        "rejected", or "timeout"."""
        self.current_decision = None
        # workflow.uuid4() is deterministic, so it's safe on replay.
        self.pending_request_id = str(workflow.uuid4())

        await workflow.execute_activity(
            notify_approval_needed,
            ApprovalRequest(
                request_id=self.pending_request_id,
                proposed_action=action,
                context=context,
                requested_at=workflow.now().isoformat(),
            ),
            start_to_close_timeout=timedelta(seconds=10),
        )

        # A durable timer: the Workflow uses no compute while it waits, and the
        # timeout survives Worker restarts.
        try:
            await workflow.wait_condition(
                lambda: self.current_decision is not None,
                timeout=timedelta(seconds=timeout_seconds),
            )
        except asyncio.TimeoutError:
            return "timeout"

        return "approved" if self.current_decision.approved else "rejected"

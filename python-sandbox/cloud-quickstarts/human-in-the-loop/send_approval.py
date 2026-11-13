import asyncio
import sys
from datetime import datetime, timezone

from temporalio.client import Client
from temporalio.contrib.pydantic import pydantic_data_converter
from temporalio.envconfig import ClientConfig

from shared import ApprovalDecision
from workflow import HumanInTheLoopWorkflow

USAGE = "Usage: uv run send_approval.py <workflow-id> <request-id> <approve|reject> [notes]"


async def main() -> None:
    if len(sys.argv) < 4 or sys.argv[3].lower() not in ("approve", "reject"):
        print(USAGE)
        sys.exit(1)

    workflow_id, request_id, decision = sys.argv[1], sys.argv[2], sys.argv[3].lower()
    notes = sys.argv[4] if len(sys.argv) > 4 else None

    # Same environment-configuration lookup the Worker and client use.
    connect_config = ClientConfig.load_client_connect_config()
    connect_config.setdefault("target_host", "localhost:7233")

    client = await Client.connect(**connect_config, data_converter=pydantic_data_converter)

    # Deliver the reviewer's decision to the waiting Workflow as a Signal.
    handle = client.get_workflow_handle(workflow_id)
    await handle.signal(
        HumanInTheLoopWorkflow.approval_decision,
        ApprovalDecision(
            request_id=request_id,
            approved=decision == "approve",
            reviewer_notes=notes,
            decided_at=datetime.now(timezone.utc).isoformat(),
        ),
    )

    print(f"Sent {decision} decision to Workflow {workflow_id}")


if __name__ == "__main__":
    asyncio.run(main())

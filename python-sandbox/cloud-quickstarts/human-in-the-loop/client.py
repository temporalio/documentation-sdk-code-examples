import asyncio
import sys

from temporalio.client import Client
from temporalio.exceptions import WorkflowAlreadyStartedError
from temporalio.contrib.pydantic import pydantic_data_converter
from temporalio.envconfig import ClientConfig

from shared import TASK_QUEUE, WORKFLOW_ID, WorkflowInput
from workflow import HumanInTheLoopWorkflow

DEFAULT_REQUEST = "Delete all test data from the production database"


async def main() -> None:
    user_request = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_REQUEST

    # Same environment-configuration lookup the Worker uses: TEMPORAL_PROFILE
    # selects the profile, the TOML file supplies address/namespace/credentials,
    # and TEMPORAL_* env vars can override individual fields.
    connect_config = ClientConfig.load_client_connect_config()
    connect_config.setdefault("target_host", "localhost:7233")

    client = await Client.connect(**connect_config, data_converter=pydantic_data_converter)

    try:
        handle = await client.start_workflow(
            HumanInTheLoopWorkflow.run,
            WorkflowInput(user_request=user_request, approval_timeout_seconds=300),
            id=WORKFLOW_ID,
            task_queue=TASK_QUEUE,
        )
    except WorkflowAlreadyStartedError:
        print(
            f"Workflow {WORKFLOW_ID} is already running. Approve or reject it, wait "
            "for it to time out, or terminate it:\n"
            f"  temporal workflow terminate --workflow-id {WORKFLOW_ID}"
        )
        sys.exit(1)

    address = connect_config.get("target_host")
    namespace = connect_config.get("namespace") or "default"
    print(
        f"Started Workflow {handle.id} against {address} (namespace: {namespace})..."
    )
    print(f"Request: {user_request}")
    print("If the action is risky, the Worker prints how to approve or reject it.")

    result = await handle.result()
    print(f"Result: {result}")


if __name__ == "__main__":
    asyncio.run(main())

"""Runs the caller Worker in the caller Namespace and starts the caller Workflow."""

import asyncio
import uuid

from temporalio.client import Client
from temporalio.worker import Worker

from nexus_feature_guide.caller_workflows import CallerWorkflow

NAMESPACE = "my-caller-namespace"
TASK_QUEUE = "my-caller-task-queue"


async def main() -> None:
    client = await Client.connect("localhost:7233", namespace=NAMESPACE)
    async with Worker(client, task_queue=TASK_QUEUE, workflows=[CallerWorkflow]):
        sync_result, wf_result = await client.execute_workflow(
            CallerWorkflow.run,
            "Nexus",
            id=f"nexus-caller-{uuid.uuid4()}",
            task_queue=TASK_QUEUE,
        )
        print(f"Sync Operation result: {sync_result.message}")
        print(f"Workflow Operation result: {wf_result.message}")


if __name__ == "__main__":
    asyncio.run(main())

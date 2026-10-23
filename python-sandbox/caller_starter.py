# @@@SNIPSTART python-nexus-quickstart-caller-starter
import asyncio
import uuid

from temporalio.client import Client
from temporalio.worker import Worker

from caller import CallerWorkflow

CALLER_TASK_QUEUE = "my-caller-task-queue"
NAMESPACE = "my-caller-namespace"


async def main():
    client = await Client.connect("localhost:7233", namespace=NAMESPACE)

    async with Worker(
        client,
        task_queue=CALLER_TASK_QUEUE,
        workflows=[CallerWorkflow],
    ):
        result = await client.execute_workflow(
            CallerWorkflow.run,
            "Temporal",
            id=f"caller-workflow-{uuid.uuid4()}",
            task_queue=CALLER_TASK_QUEUE,
        )
        print("Workflow result:", result)


if __name__ == "__main__":
    asyncio.run(main())
# @@@SNIPEND

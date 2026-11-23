# @@@SNIPSTART python-client-sync-asyncio-run
import asyncio

from temporalio.client import Client

from temporal_client.your_workflows import YourWorkflow


async def start_your_workflow(name: str) -> str:
    client = await Client.connect("localhost:7233")
    handle = await client.start_workflow(
        YourWorkflow.run,
        name,
        id=f"your-workflow-{name}",
        task_queue="your-task-queue",
    )
    return handle.id


def start_from_sync_code(name: str) -> None:
    workflow_id = asyncio.run(start_your_workflow(name))
    print(f"Started Workflow {workflow_id}")
# @@@SNIPEND


if __name__ == "__main__":
    start_from_sync_code("your-name")

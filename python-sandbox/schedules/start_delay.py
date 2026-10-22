# @@@SNIPSTART python-schedules-start-delay
import asyncio
from datetime import timedelta

from temporalio.client import Client

from schedules.your_workflows import YourWorkflow


async def main():
    client = await Client.connect("localhost:7233")

    result = await client.execute_workflow(
        YourWorkflow.run,
        "your name",
        id="your-workflow-id",
        task_queue="your-task-queue",
        start_delay=timedelta(hours=1, minutes=20, seconds=30)
    )

    print(f"Result: {result}")


if __name__ == "__main__":
    asyncio.run(main())
# @@@SNIPEND

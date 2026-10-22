# @@@SNIPSTART python-workflow-timeouts-starter
import asyncio
from datetime import timedelta
from temporalio.client import Client
from workflow_timeouts.your_workflows import YourWorkflow
from temporalio.common import RetryPolicy


async def main():
    client = await Client.connect("localhost:7233")

    result = await client.execute_workflow(
        YourWorkflow.run,
        "your timeout argument",
        id="your-workflow-id",
        task_queue="your-task-queue",
        # Set Workflow Timeout duration
        execution_timeout=timedelta(seconds=2),
        # run_timeout=timedelta(seconds=2),
        # task_timeout=timedelta(seconds=2),
    )
    handle = await client.start_workflow(
        YourWorkflow.run,
        "your retry policy argument",
        id="your-workflow-id",
        task_queue="your-task-queue",
        retry_policy=RetryPolicy(maximum_interval=timedelta(seconds=2)),
    )

    print(f"Result: {result}")
    print(f"Handle: {handle.id}")


if __name__ == "__main__":
    asyncio.run(main())
# @@@SNIPEND

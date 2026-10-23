# @@@SNIPSTART python-schedules-cron
import asyncio

from temporalio.client import Client

from schedules.your_workflows import CronWorkflow


async def main():
    client = await Client.connect("localhost:7233")

    handle = await client.start_workflow(
        CronWorkflow.run,
        id="your-workflow-id",
        task_queue="your-task-queue",
        cron_schedule="* * * * *",
    )
    print(f"Started Cron Workflow: {handle.id}")


if __name__ == "__main__":
    asyncio.run(main())
# @@@SNIPEND

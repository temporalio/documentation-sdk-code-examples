import asyncio
from concurrent.futures import ThreadPoolExecutor

from temporalio.client import Client
from temporalio.worker import Worker

from run_worker.hello import MyWorkflow, my_sync_activity


async def main():
    client = await Client.connect("localhost:7233")
    # @@@SNIPSTART python-run-worker-sync-activities
    worker = Worker(
        client,
        task_queue="my-task-queue",
        workflows=[MyWorkflow],
        activities=[my_sync_activity],
        activity_executor=ThreadPoolExecutor(5),
    )
    # @@@SNIPEND
    await worker.run()


if __name__ == "__main__":
    asyncio.run(main())

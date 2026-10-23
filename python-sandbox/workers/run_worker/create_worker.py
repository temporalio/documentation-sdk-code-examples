import asyncio

from temporalio.client import Client
from temporalio.worker import Worker

from run_worker.hello import HelloWorkflow, some_activity


async def main():
    # @@@SNIPSTART python-run-worker-create
    client = await Client.connect("localhost:7233")

    worker = Worker(
        client,
        task_queue="my-task-queue",
        workflows=[HelloWorkflow],
        activities=[some_activity],
    )
    await worker.run()
    # @@@SNIPEND


if __name__ == "__main__":
    asyncio.run(main())

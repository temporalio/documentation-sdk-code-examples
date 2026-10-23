import asyncio
from datetime import timedelta

from temporalio.client import Client
from temporalio.worker import Worker

from run_worker.hello import HelloWorkflow, some_activity


async def main():
    client = await Client.connect("localhost:7233")
    # @@@SNIPSTART python-run-worker-graceful-shutdown
    worker = Worker(
        client,
        task_queue="my-task-queue",
        workflows=[HelloWorkflow],
        activities=[some_activity],
        graceful_shutdown_timeout=timedelta(seconds=30),
    )
    async with worker:
        await interrupt_event.wait()
    # @@@SNIPEND


# @@@SNIPSTART python-run-worker-interrupt-entrypoint
interrupt_event = asyncio.Event()

if __name__ == "__main__":
    loop = asyncio.new_event_loop()
    try:
        loop.run_until_complete(main())
    except KeyboardInterrupt:
        interrupt_event.set()
        loop.run_until_complete(loop.shutdown_asyncgens())
# @@@SNIPEND

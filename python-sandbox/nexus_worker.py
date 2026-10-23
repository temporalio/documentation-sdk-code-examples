# @@@SNIPSTART python-nexus-quickstart-worker
import asyncio

from temporalio.client import Client
from temporalio.worker import Worker

from workflows import SayHelloWorkflow
from activities import greet
from handler import SayHelloNexusServiceHandler


async def main():
    client = await Client.connect("localhost:7233")
    worker = Worker(
        client,
        task_queue="my-task-queue",
        workflows=[SayHelloWorkflow],
        activities=[greet],
        nexus_service_handlers=[SayHelloNexusServiceHandler()],
    )
    print("Worker started.")
    await worker.run()


if __name__ == "__main__":
    asyncio.run(main())
# @@@SNIPEND

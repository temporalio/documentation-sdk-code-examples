import asyncio

from temporalio.client import Client
from temporalio.worker import Worker

from versioning.pizza_workflows import PizzaWorkflow, PizzaWorkflowV2


async def main():
    client = await Client.connect("localhost:7233")
    # @@@SNIPSTART python-versioning-cutover-worker
    worker = Worker(
        client,
        task_queue="your-task-queue",
        workflows=[PizzaWorkflow, PizzaWorkflowV2],
    )
    # @@@SNIPEND
    await worker.run()


if __name__ == "__main__":
    asyncio.run(main())

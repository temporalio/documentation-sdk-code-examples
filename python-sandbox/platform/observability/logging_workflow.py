import asyncio
import logging

from temporalio.client import Client
from temporalio.worker import Worker


# @@@SNIPSTART python-observability-logging-workflow
from temporalio import workflow


@workflow.defn
class GreetingWorkflow:
    def __init__(self) -> None:
        self._greeting = "<no greeting>"

    @workflow.run
    async def run(self, name: str) -> None:
        workflow.logger.info("Workflow input parameter: %s" % name)
        self._greeting = f"Hello, {name}!"

    @workflow.query
    def greeting(self) -> str:
        return self._greeting
# @@@SNIPEND


async def main():
    # @@@SNIPSTART python-observability-logging-config
    logging.basicConfig(level=logging.INFO)
    # @@@SNIPEND
    client = await Client.connect("localhost:7233")
    worker = Worker(client, task_queue="logging-task-queue", workflows=[GreetingWorkflow])
    await worker.run()


if __name__ == "__main__":
    asyncio.run(main())

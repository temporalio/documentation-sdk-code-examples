import asyncio

from temporalio.client import Client
from temporalio.worker import Worker

from nexus_feature_guide.handler_workflows import (
    HelloArgsHandlerWorkflow,
    HelloHandlerWorkflow,
)
from nexus_feature_guide.service_handler import MyNexusServiceHandler

NAMESPACE = "my-target-namespace"
TASK_QUEUE = "my-handler-task-queue"


# @@@SNIPSTART python-nexus-feature-guide-handler-worker
async def main():
    client = await Client.connect("localhost:7233", namespace=NAMESPACE)
    worker = Worker(
        client,
        task_queue=TASK_QUEUE,
        workflows=[HelloHandlerWorkflow, HelloArgsHandlerWorkflow],
        nexus_service_handlers=[MyNexusServiceHandler()],
    )
    await worker.run()
# @@@SNIPEND


if __name__ == "__main__":
    asyncio.run(main())

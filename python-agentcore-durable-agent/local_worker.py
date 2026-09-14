import asyncio
from concurrent.futures import ThreadPoolExecutor

from strands.models import BedrockModel
from temporalio.client import Client
from temporalio.contrib.strands import StrandsPlugin
from temporalio.worker import Worker

from activities import execute_code
from workflows import MODEL_ID, MODEL_NAME, TASK_QUEUE, DurableAgentWorkflow


# @@@SNIPSTART python-agentcore-durable-agent-local-worker
async def main() -> None:
    client = await Client.connect(
        "localhost:7233",
        plugins=[
            StrandsPlugin(models={MODEL_NAME: lambda: BedrockModel(model_id=MODEL_ID)})
        ],
    )

    with ThreadPoolExecutor(max_workers=4) as activity_executor:
        worker = Worker(
            client,
            task_queue=TASK_QUEUE,
            workflows=[DurableAgentWorkflow],
            activities=[execute_code],
            activity_executor=activity_executor,
        )
        await worker.run()


# @@@SNIPEND


if __name__ == "__main__":
    asyncio.run(main())

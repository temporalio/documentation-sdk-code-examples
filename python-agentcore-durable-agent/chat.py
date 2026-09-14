import asyncio
import uuid

from strands.models import BedrockModel
from temporalio.client import Client
from temporalio.contrib.strands import StrandsPlugin

from workflows import MODEL_ID, MODEL_NAME, TASK_QUEUE, DurableAgentWorkflow


# @@@SNIPSTART python-agentcore-durable-agent-chat-client
async def main() -> None:
    client = await Client.connect(
        "localhost:7233",
        plugins=[
            StrandsPlugin(models={MODEL_NAME: lambda: BedrockModel(model_id=MODEL_ID)})
        ],
    )
    handle = await client.start_workflow(
        DurableAgentWorkflow.run,
        id=f"durable-agent-{uuid.uuid4()}",
        task_queue=TASK_QUEUE,
    )

    while prompt := input("You: "):
        if prompt == "/finish":
            await handle.signal(DurableAgentWorkflow.finish)
            return
        answer = await handle.execute_update(DurableAgentWorkflow.ask, prompt)
        print(f"Agent: {answer}")


# @@@SNIPEND


if __name__ == "__main__":
    asyncio.run(main())

import asyncio
import uuid

from temporalio.client import Client
from temporalio.contrib.strands import StrandsPlugin

from workflows import TASK_QUEUE, DurableAgentWorkflow


# @@@SNIPSTART python-agentcore-durable-agent-chat-client
async def main() -> None:
    client = await Client.connect(
        "localhost:7233",
        plugins=[StrandsPlugin()],
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

# @@@SNIPSTART python-message-passing-signal-with-start
import asyncio

from temporalio.client import Client
from message_passing.wf_signal import GreetingWorkflow

async def main():
    client = await Client.connect("localhost:7233")
    await client.start_workflow(
        GreetingWorkflow.run,
        id="your-signal-with-start-workflow",
        task_queue="signal-tq",
        start_signal="submit_greeting",
        start_signal_args=["User Signal with Start"],
    )
    handle = client.get_workflow_handle_for(
        GreetingWorkflow, "your-signal-with-start-workflow"
    )
    await handle.signal(GreetingWorkflow.exit)
    result = await handle.result()
    print(result)
# @@@SNIPEND


if __name__ == "__main__":
    asyncio.run(main())

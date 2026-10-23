# @@@SNIPSTART python-client-get-workflow-result
import asyncio
from temporalio.client import Client


async def main():
    client = await Client.connect("localhost:7233")

    handle = client.get_workflow_handle(
        workflow_id="your-workflow-id",
    )
    results = await handle.result()
    print(f"Result: {results}")


if __name__ == "__main__":
    asyncio.run(main())
# @@@SNIPEND

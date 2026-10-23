# @@@SNIPSTART python-observability-list-workflows
import asyncio
from temporalio.client import Client
from observability.search_attributes_workflow import GreetingWorkflow


async def main():
    client = await Client.connect("localhost:7233")
    handle = await client.start_workflow(
        GreetingWorkflow.run,
        id="search-attributes-workflow-id",
        task_queue="search-attributes-task-queue",
        search_attributes={"CustomKeywordField": ["old-value"]},
    )

    print(
        "First search attribute values: ",
        (await handle.describe()).search_attributes.get("CustomKeywordField"),
    )
    await asyncio.sleep(3)
    print(
        "Second search attribute values: ",
        (await handle.describe()).search_attributes.get("CustomKeywordField"),
    )
    await asyncio.sleep(3)
    print(
        "Empty search attribute values: ",
        (await handle.describe()).search_attributes.get("CustomKeywordField"),
    )

    async for workflow in client.list_workflows('WorkflowType="GreetingWorkflow"'):
        print(f"Workflow: {workflow.id}")


if __name__ == "__main__":
    asyncio.run(main())
# @@@SNIPEND

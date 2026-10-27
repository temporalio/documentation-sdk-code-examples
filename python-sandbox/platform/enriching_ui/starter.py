from temporalio.client import Client

from enriching_ui.details_workflow import YourWorkflow


async def start_with_summary(client: Client):
    # @@@SNIPSTART python-enriching-ui-start-workflow
    # Start a Workflow with static summary and details
    handle = await client.start_workflow(
        YourWorkflow.run,
        "workflow input",
        id="your-workflow-id",
        task_queue="your-task-queue",
        static_summary="Order processing for customer #12345",
        static_details="Processing premium order with expedited shipping"
    )
    # @@@SNIPEND
    return handle


async def execute_with_summary(client: Client) -> str:
    # @@@SNIPSTART python-enriching-ui-execute-workflow
    result = await client.execute_workflow(
        YourWorkflow.run,
        "workflow input",
        id="your-workflow-id",
        task_queue="your-task-queue",
        static_summary="Order processing for customer #12345",
        static_details="Processing premium order with expedited shipping"
    )
    # @@@SNIPEND
    return result

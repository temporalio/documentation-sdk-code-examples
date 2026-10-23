from temporalio.client import Client


async def cancel_workflow(client: Client, workflow_id: str) -> None:
    # @@@SNIPSTART python-cancellation-cancel-workflow
    await client.get_workflow_handle(workflow_id).cancel()
    # @@@SNIPEND


async def terminate_workflow(client: Client, workflow_id: str) -> None:
    # @@@SNIPSTART python-cancellation-terminate-workflow
    await client.get_workflow_handle(workflow_id).terminate()
    # @@@SNIPEND

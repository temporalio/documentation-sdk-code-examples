from temporalio.client import Client


async def complete_activity(my_client: Client, captured_token: bytes) -> None:
    # @@@SNIPSTART python-async-activity-completion-get-handle
    handle = my_client.get_async_activity_handle(task_token=captured_token)
    # @@@SNIPEND
    # Report progress while the external work is in flight.
    await handle.heartbeat("still working")
    # @@@SNIPSTART python-async-activity-completion-complete
    await handle.complete("Completion value.")
    # @@@SNIPEND

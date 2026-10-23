import asyncio

from temporalio import activity

# Stand-in for an external system, such as a job queue or a ticketing system,
# that completes the Activity later.
pending_completions: asyncio.Queue[bytes] = asyncio.Queue()


@activity.defn
async def request_approval(request: str) -> str:
    activity.logger.info("Sending %s to the external system", request)
    # @@@SNIPSTART python-async-activity-completion-raise
    # Capture token for later completion
    captured_token = activity.info().task_token
    pending_completions.put_nowait(captured_token)  # Give the external system the token
    activity.raise_complete_async()
    # @@@SNIPEND

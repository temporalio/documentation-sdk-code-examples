# @@@SNIPSTART python-modal-worker-activities
from temporalio import activity


@activity.defn
async def greet(name: str) -> str:
    activity.logger.info("Processing greeting")
    return f"Hello, {name}!"
# @@@SNIPEND

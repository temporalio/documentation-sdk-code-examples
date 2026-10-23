# @@@SNIPSTART python-benign-exceptions-activity
from temporalio import activity
from temporalio.exceptions import ApplicationError, ApplicationErrorCategory

@activity.defn
async def my_activity() -> str:
    try:
        return await call_external_service()
    except Exception as err:
        raise ApplicationError(
            message=str(err),
            # Mark this error as benign since it's expected
            category=ApplicationErrorCategory.BENIGN,
        )
# @@@SNIPEND


async def call_external_service() -> str:
    # Pretend that we are polling an external service that isn't ready until the third attempt.
    if activity.info().attempt < 3:
        raise RuntimeError("external service isn't ready yet")
    return "external service is ready"

from dataclasses import dataclass

from temporalio import activity


@dataclass
class MyActivityInput:
    # The attempt that succeeds. Earlier attempts fail to show the retry delay.
    succeed_on_attempt: int


async def call_remote_service(input: MyActivityInput) -> str:
    # Pretend that we are calling a remote service that fails until it recovers.
    if activity.info().attempt < input.succeed_on_attempt:
        raise ConnectionError("remote service unavailable")
    return f"succeeded on attempt {activity.info().attempt}"


# @@@SNIPSTART python-activity-timeouts-next-retry-delay
from temporalio.exceptions import ApplicationError
from datetime import timedelta

@activity.defn
async def my_activity(input: MyActivityInput) -> str:
    try:
        # Your activity logic goes here
        return await call_remote_service(input)
    except Exception as e:
        attempt = activity.info().attempt
        raise ApplicationError(
            f"Error encountered on attempt {attempt}",
            next_retry_delay=timedelta(seconds=3 * attempt),
        ) from e
# @@@SNIPEND

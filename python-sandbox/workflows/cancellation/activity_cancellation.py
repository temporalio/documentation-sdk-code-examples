import asyncio
from datetime import timedelta
from typing import NoReturn

from temporalio import activity, workflow

with workflow.unsafe.imports_passed_through():
    from cancellation.dataobject import ComposeArgsInput


# @@@SNIPSTART python-cancellation-activity
@activity.defn
async def cancellable_activity(input: ComposeArgsInput) -> NoReturn:
    try:
        while True:
            print("Heartbeating cancel activity")
            await asyncio.sleep(0.5)
            activity.heartbeat("some details")
    except asyncio.CancelledError:
        print("Activity cancelled")
        raise


@activity.defn
async def run_activity(input: ComposeArgsInput):
    print("Executing activity")
    return input.arg1 + input.arg2

@workflow.defn
class GreetingWorkflow:
    @workflow.run
    async def run(self, input: ComposeArgsInput) -> None:
        activity_handle = workflow.start_activity(
            cancellable_activity,
            ComposeArgsInput(input.arg1, input.arg2),
            start_to_close_timeout=timedelta(minutes=5),
            heartbeat_timeout=timedelta(seconds=30),
        )

        await asyncio.sleep(3)
        activity_handle.cancel()
# @@@SNIPEND

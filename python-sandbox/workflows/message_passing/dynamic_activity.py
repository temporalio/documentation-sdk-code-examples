# @@@SNIPSTART python-message-passing-dynamic-activity
from dataclasses import dataclass
from datetime import timedelta
from collections.abc import Sequence

from temporalio import activity, workflow
from temporalio.common import RawValue

@dataclass
class YourDataClass:
    greeting: str
    name: str


@activity.defn(dynamic=True)
async def dynamic_greeting(args: Sequence[RawValue]) -> str:
    arg1 = activity.payload_converter().from_payload(args[0].payload, YourDataClass)
    return (
        f"{arg1.greeting}, {arg1.name}!\nActivity Type: {activity.info().activity_type}"
    )


@activity.defn()
async def default_greeting(input: YourDataClass) -> str:
    return f"{input.greeting}, {input.name}!\nActivity Type: {activity.info().activity_type}"


@workflow.defn
class GreetingWorkflow:
    @workflow.run
    async def run(self, name: str) -> str:
        return await workflow.execute_activity(
            "unregistered_activity",
            YourDataClass("Hello", name),
            start_to_close_timeout=timedelta(seconds=10),
        )
# @@@SNIPEND

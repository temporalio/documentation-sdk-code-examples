# @@@SNIPSTART python-message-passing-dynamic-workflow
from dataclasses import dataclass
from datetime import timedelta
from collections.abc import Sequence

from temporalio import activity, workflow
from temporalio.common import RawValue

@dataclass
class YourDataClass:
    greeting: str
    name: str


@activity.defn()
async def default_greeting(input: YourDataClass) -> str:
    return f"{input.greeting}, {input.name}!\nActivity Type: {activity.info().activity_type}"


@workflow.defn(dynamic=True)
class DynamicWorkflow:
    @workflow.run
    async def run(self, args: Sequence[RawValue]) -> str:
        name = workflow.payload_converter().from_payload(args[0].payload, str)
        return await workflow.execute_activity(
            default_greeting,
            YourDataClass("Hello", name),
            start_to_close_timeout=timedelta(seconds=10),
        )
# @@@SNIPEND

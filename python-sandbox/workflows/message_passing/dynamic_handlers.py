# @@@SNIPSTART python-message-passing-dynamic-signal
from collections.abc import Sequence

from temporalio.common import RawValue
from temporalio import workflow


@workflow.defn
class DynamicSignalWorkflow:
    def __init__(self) -> None:
        self.received: list[str] = []

    @workflow.run
    async def run(self) -> list[str]:
        await workflow.wait_condition(lambda: len(self.received) > 0)
        return self.received

    @workflow.signal(dynamic=True)
    async def dynamic_signal(self, name: str, args: Sequence[RawValue]) -> None:
        value = workflow.payload_converter().from_payload(args[0].payload, str)
        self.received.append(f"{name}: {value}")
# @@@SNIPEND

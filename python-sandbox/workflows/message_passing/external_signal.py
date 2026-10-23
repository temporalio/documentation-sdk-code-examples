# @@@SNIPSTART python-message-passing-external-signal
from typing import Optional

from temporalio import workflow

@workflow.defn
class WorkflowA:
    def __init__(self) -> None:
        self._signal: Optional[str] = None

    @workflow.run
    async def run(self) -> str:
        await workflow.wait_condition(lambda: self._signal is not None)
        return self._signal

    @workflow.signal
    def your_signal(self, value: str) -> None:
        self._signal = value


@workflow.defn
class WorkflowB:
    @workflow.run
    async def run(self) -> None:
        handle = workflow.get_external_workflow_handle_for(WorkflowA.run, "workflow-a")
        await handle.signal(WorkflowA.your_signal, "signal argument")
# @@@SNIPEND

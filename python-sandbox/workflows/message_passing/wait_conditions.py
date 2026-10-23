from dataclasses import dataclass
from datetime import timedelta

from temporalio import workflow


@dataclass
class UpdateInput:
    value: str


# @@@SNIPSTART python-message-passing-wait-all-handlers
@workflow.defn
class MyWorkflow:
    @workflow.run
    async def run(self) -> str:
        await workflow.wait_condition(lambda: self.ready)
        await workflow.wait_condition(workflow.all_handlers_finished)
        return "workflow-result"
    # @@@SNIPEND

    def __init__(self) -> None:
        self.ready = False
        self.values: list[str] = []

    @workflow.signal
    def start_processing(self) -> None:
        self.ready = True

    # @@@SNIPSTART python-message-passing-wait-in-handler
    @workflow.update
    async def my_update(self, update_input: UpdateInput) -> str:
        await workflow.wait_condition(
            lambda: self.ready_for_update_to_execute(update_input)
        )
        # @@@SNIPEND
        self.values.append(update_input.value)
        return update_input.value

    def ready_for_update_to_execute(self, update_input: UpdateInput) -> bool:
        return self.ready and bool(update_input.value)


@workflow.defn
class AbandonUpdateWorkflow:
    def __init__(self) -> None:
        self.done = False

    @workflow.run
    async def run(self) -> str:
        # This Workflow doesn't wait for handlers to finish, so it can complete
        # while my_update is still running.
        await workflow.wait_condition(lambda: self.done)
        return "workflow-result"

    @workflow.signal
    def finish(self) -> None:
        self.done = True

    # @@@SNIPSTART python-message-passing-unfinished-policy
    @workflow.update(unfinished_policy=workflow.HandlerUnfinishedPolicy.ABANDON)
    async def my_update(self) -> None:
        await workflow.sleep(timedelta(hours=1))
    # @@@SNIPEND

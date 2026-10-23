# @@@SNIPSTART python-workflow-streams-continue-as-new
from dataclasses import dataclass, field

from temporalio import workflow
from temporalio.contrib.workflow_streams import WorkflowStream, WorkflowStreamState

with workflow.unsafe.imports_passed_through():
    from workflow_streams.long_running_work import do_one_iteration


@dataclass
class AppState:
    items_processed: int = 0


@dataclass
class WorkflowInput:
    app_state: AppState = field(default_factory=AppState)
    stream_state: WorkflowStreamState | None = None


@workflow.defn
class LongRunningWorkflow:
    @workflow.init
    def __init__(self, input: WorkflowInput) -> None:
        self.app_state = input.app_state
        self.stream = WorkflowStream(prior_state=input.stream_state)

    @workflow.run
    async def run(self, input: WorkflowInput) -> None:
        while True:
            await do_one_iteration(self)
            if workflow.info().is_continue_as_new_suggested():
                await self.stream.continue_as_new(
                    lambda stream_state: [
                        WorkflowInput(
                            app_state=self.app_state,
                            stream_state=stream_state,
                        )
                    ]
                )
# @@@SNIPEND


@workflow.defn
class ExplicitRolloverWorkflow:
    """Rolls over to another Task Queue once, using the explicit Continue-As-New recipe."""

    @workflow.init
    def __init__(self, input: WorkflowInput) -> None:
        self.app_state = input.app_state
        self.stream = WorkflowStream(prior_state=input.stream_state)

    @workflow.run
    async def run(self, input: WorkflowInput) -> int:
        await do_one_iteration(self)
        if workflow.info().task_queue != "other-tq":
            # @@@SNIPSTART python-workflow-streams-explicit-continue-as-new
            self.stream.detach_pollers()
            await workflow.wait_condition(workflow.all_handlers_finished)
            workflow.continue_as_new(
                args=[WorkflowInput(app_state=self.app_state, stream_state=self.stream.get_state())],
                task_queue="other-tq",
            )
            # @@@SNIPEND
        return self.app_state.items_processed

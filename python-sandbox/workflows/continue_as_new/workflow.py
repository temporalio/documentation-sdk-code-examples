from dataclasses import dataclass
from datetime import timedelta
from typing import Optional

from temporalio import workflow

# The total work the Workflow does across all runs in the chain.
JOBS_TO_PROCESS = 50


@dataclass
class ClusterManagerState:
    jobs_processed: int = 0


@dataclass
class ClusterManagerResult:
    jobs_processed: int


# @@@SNIPSTART python-continue-as-new-input
@dataclass
class ClusterManagerInput:
    state: Optional[ClusterManagerState] = None
    test_continue_as_new: bool = False


@workflow.defn
class ClusterManagerWorkflow:
    @workflow.init
    def __init__(self, input: ClusterManagerInput) -> None:
        # The original caller passes state=None. Later runs receive the state from the previous run.
        self.state = input.state or ClusterManagerState()
        # Test hook: use a small history limit so tests reach Continue-As-New quickly.
        self.max_history_length: Optional[int] = (
            120 if input.test_continue_as_new else None
        )

    @workflow.run
    async def run(self, input: ClusterManagerInput) -> ClusterManagerResult:
        # @@@SNIPEND
        while self.state.jobs_processed < JOBS_TO_PROCESS:
            # Stand-in for a unit of work. Each Timer adds Events to the Event History.
            await workflow.sleep(timedelta(seconds=1))
            self.state.jobs_processed += 1

            if self.should_continue_as_new():
                # Let any Signal or Update handlers finish before continuing as new.
                await workflow.wait_condition(workflow.all_handlers_finished)
                # @@@SNIPSTART python-continue-as-new-call
                workflow.continue_as_new(
                    ClusterManagerInput(
                        state=self.state,
                        test_continue_as_new=input.test_continue_as_new,
                    )
                )
                # @@@SNIPEND
        return ClusterManagerResult(jobs_processed=self.state.jobs_processed)

    # @@@SNIPSTART python-continue-as-new-should-continue
    def should_continue_as_new(self) -> bool:
        if workflow.info().is_continue_as_new_suggested():
            return True
        # For testing
        if (
            self.max_history_length
            and workflow.info().get_current_history_length() > self.max_history_length
        ):
            return True
        return False
    # @@@SNIPEND

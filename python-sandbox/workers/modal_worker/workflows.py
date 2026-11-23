# @@@SNIPSTART python-modal-worker-workflows
from datetime import timedelta

from temporalio import workflow
from temporalio.common import VersioningBehavior

with workflow.unsafe.imports_passed_through():
    from modal_worker.activities import greet


@workflow.defn(versioning_behavior=VersioningBehavior.PINNED)
class GreetingWorkflow:
    @workflow.run
    async def run(self, name: str) -> str:
        return await workflow.execute_activity(
            greet,
            name,
            start_to_close_timeout=timedelta(seconds=30),
        )
# @@@SNIPEND

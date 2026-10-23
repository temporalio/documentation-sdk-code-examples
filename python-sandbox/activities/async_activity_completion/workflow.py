from datetime import timedelta

from temporalio import workflow

with workflow.unsafe.imports_passed_through():
    from async_activity_completion.activities import request_approval


@workflow.defn
class ApprovalWorkflow:
    @workflow.run
    async def run(self, request: str) -> str:
        return await workflow.execute_activity(
            request_approval,
            request,
            start_to_close_timeout=timedelta(minutes=5),
            heartbeat_timeout=timedelta(minutes=1),
        )

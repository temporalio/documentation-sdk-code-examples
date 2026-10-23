from datetime import timedelta

from temporalio import workflow

with workflow.unsafe.imports_passed_through():
    from agentcore_worker.activities import my_activity

DEPLOYMENT_NAME = "agentcore-worker"
BUILD_ID = "1.0"
TASK_QUEUE = "agentcore-task-queue"


@workflow.defn
class ProcessItemsWorkflow:
    @workflow.run
    async def run(self, items: list[str]) -> str:
        return await workflow.execute_activity(
            my_activity,
            items,
            start_to_close_timeout=timedelta(minutes=5),
            heartbeat_timeout=timedelta(seconds=30),
        )

# @@@SNIPSTART python-lambda-worker-sandbox-workflow
from datetime import timedelta

from temporalio import workflow
from temporalio.common import VersioningBehavior

with workflow.unsafe.imports_passed_through():
    from aws_lambda_worker.activities import hello_activity

TASK_QUEUE = "serverless-task-queue-python"


@workflow.defn(versioning_behavior=VersioningBehavior.PINNED)
class SampleWorkflow:
    @workflow.run
    async def run(self, name: str) -> str:
        return await workflow.execute_activity(
            hello_activity,
            name,
            start_to_close_timeout=timedelta(seconds=10),
        )
# @@@SNIPEND

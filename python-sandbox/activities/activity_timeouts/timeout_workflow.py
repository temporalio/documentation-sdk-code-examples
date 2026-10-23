# @@@SNIPSTART python-activity-timeouts-timeout-workflow
from datetime import timedelta

from temporalio import workflow

with workflow.unsafe.imports_passed_through():
    from activity_timeouts.activities import your_activity, YourParams

@workflow.defn
class YourWorkflow:
    @workflow.run
    async def run(self, greeting: str) -> str:
        activity_timeout_result = await workflow.execute_activity(
            your_activity,
            YourParams(greeting, "Activity Timeout option"),
            # Activity Execution Timeout
            start_to_close_timeout=timedelta(seconds=10),
            # schedule_to_start_timeout=timedelta(seconds=10),
            # schedule_to_close_timeout=timedelta(seconds=10),
        )
        return activity_timeout_result
# @@@SNIPEND

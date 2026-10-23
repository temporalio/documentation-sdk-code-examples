# @@@SNIPSTART python-activity-timeouts-retry-workflow
from datetime import timedelta

from temporalio import workflow
from temporalio.common import RetryPolicy

with workflow.unsafe.imports_passed_through():
    from activity_timeouts.activities import your_activity, YourParams

@workflow.defn
class YourWorkflow:
    @workflow.run
    async def run(self, greeting: str) -> str:
        activity_result = await workflow.execute_activity(
            your_activity,
            YourParams(greeting, "Retry Policy options"),
            start_to_close_timeout=timedelta(seconds=10),
            # Retry Policy
            retry_policy=RetryPolicy(
                backoff_coefficient=2.0,
                maximum_attempts=5,
                initial_interval=timedelta(seconds=1),
                maximum_interval=timedelta(seconds=2),
                # non_retryable_error_types=["ValueError"],
            ),
        )
        return activity_result
# @@@SNIPEND

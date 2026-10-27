from datetime import timedelta

from temporalio import workflow

with workflow.unsafe.imports_passed_through():
    from enriching_ui.activities import your_activity


# @@@SNIPSTART python-enriching-ui-activity-summary
@workflow.defn
class YourWorkflow:
    @workflow.run
    async def run(self, input: str) -> str:
        # Start an activity with a summary
        result = await workflow.execute_activity(
            your_activity,
            input,
            start_to_close_timeout=timedelta(seconds=10),
            summary="Processing user data"
        )
        return result
# @@@SNIPEND

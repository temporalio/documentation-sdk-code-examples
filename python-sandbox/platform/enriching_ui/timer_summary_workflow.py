from datetime import timedelta

from temporalio import workflow


# @@@SNIPSTART python-enriching-ui-timer-summary
@workflow.defn
class YourWorkflow:
    @workflow.run
    async def run(self, input: str) -> str:
        # Create a timer with a summary
        await workflow.sleep(timedelta(minutes=5), summary="Waiting for payment confirmation")
        return "Timer completed"
# @@@SNIPEND

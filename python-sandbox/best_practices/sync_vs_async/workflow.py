from datetime import timedelta

from temporalio import workflow


@workflow.defn
class GreetingWorkflow:
    @workflow.run
    async def run(self, name: str) -> str:
        # Call the Activity by name so the same Workflow works with every implementation.
        return await workflow.execute_activity(
            "greet_in_spanish", name, start_to_close_timeout=timedelta(seconds=10)
        )

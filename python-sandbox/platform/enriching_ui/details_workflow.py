from temporalio import workflow


# @@@SNIPSTART python-enriching-ui-current-details
@workflow.defn
class YourWorkflow:
    @workflow.run
    async def run(self, input: str) -> str:
        # Get the current details
        current_details = workflow.get_current_details()
        print(f"Current details: {current_details}")

        # Set/update the current details
        workflow.set_current_details("Updated workflow details with new status")

        return "Workflow completed"
# @@@SNIPEND

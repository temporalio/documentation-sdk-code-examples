from temporalio import workflow


# @@@SNIPSTART python-versioning-cutover-workflows
@workflow.defn(name="PizzaWorkflow")
class PizzaWorkflow:
    @workflow.run
    async def run(self, name: str) -> str:
        # this function contains the original code
        return f"Pizza for {name}"

@workflow.defn(name="PizzaWorkflowV2")
class PizzaWorkflowV2:
    @workflow.run
    async def run(self, name: str) -> str:
        # this function contains the updated code
        return f"Pizza with extra cheese for {name}"
# @@@SNIPEND

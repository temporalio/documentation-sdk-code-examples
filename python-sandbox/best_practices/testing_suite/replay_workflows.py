from temporalio import workflow


@workflow.defn
class MyWorkflowA:
    @workflow.run
    async def run(self, name: str) -> str:
        return f"A: {name}"


@workflow.defn
class MyWorkflowB:
    @workflow.run
    async def run(self, name: str) -> str:
        return f"B: {name}"


@workflow.defn
class MyWorkflowC:
    @workflow.run
    async def run(self, name: str) -> str:
        return f"C: {name}"


@workflow.defn
class YourWorkflow:
    @workflow.run
    async def run(self, name: str) -> str:
        return f"Hello, {name}!"

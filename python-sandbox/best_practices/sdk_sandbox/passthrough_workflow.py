# @@@SNIPSTART python-sdk-sandbox-passthrough-workflow
from temporalio import workflow

with workflow.unsafe.imports_passed_through():
    import pydantic

@workflow.defn
class MyWorkflow:
    @workflow.run
    async def run(self, name: str) -> str:
        greeting = pydantic.TypeAdapter(str).validate_python(f"Hello, {name}!")
        return greeting
# @@@SNIPEND

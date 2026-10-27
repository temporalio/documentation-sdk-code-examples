# @@@SNIPSTART python-sdk-sandbox-notification-workflow
from temporalio import workflow

with workflow.unsafe.sandbox_import_notification_policy(
    workflow.SandboxImportNotificationPolicy.SILENT
):
    import pydantic

@workflow.defn
class MyWorkflow:
    @workflow.run
    async def run(self, name: str) -> str:
        greeting = pydantic.TypeAdapter(str).validate_python(f"Hello, {name}!")
        return greeting
# @@@SNIPEND

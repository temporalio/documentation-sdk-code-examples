import datetime

from temporalio import workflow


# @@@SNIPSTART python-sdk-sandbox-unsandboxed-workflow
@workflow.defn(sandboxed=False)
# @@@SNIPEND
class UnsandboxedWorkflow:
    @workflow.run
    async def run(self) -> str:
        # Without the sandbox, restricted calls like this one aren't blocked.
        return datetime.date.today().isoformat()

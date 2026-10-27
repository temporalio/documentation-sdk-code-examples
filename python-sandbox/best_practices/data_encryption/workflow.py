from temporalio import workflow


@workflow.defn
class EchoWorkflow:
    @workflow.run
    async def run(self, message: str) -> str:
        return message

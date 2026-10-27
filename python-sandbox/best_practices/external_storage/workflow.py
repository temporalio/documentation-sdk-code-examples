from temporalio import workflow


@workflow.defn
class LargePayloadWorkflow:
    @workflow.run
    async def run(self, data: str) -> str:
        return data.upper()

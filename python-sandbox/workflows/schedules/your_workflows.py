from temporalio import workflow


@workflow.defn
class YourSchedulesWorkflow:
    @workflow.run
    async def run(self, name: str) -> str:
        return f"Hello, {name}!"


@workflow.defn
class CronWorkflow:
    @workflow.run
    async def run(self) -> str:
        return "Hello from a Cron Workflow!"


@workflow.defn
class YourWorkflow:
    @workflow.run
    async def run(self, name: str) -> str:
        return f"Hello, {name}!"

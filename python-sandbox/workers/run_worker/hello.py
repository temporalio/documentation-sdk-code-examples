import time
from datetime import timedelta

from temporalio import activity, workflow


@activity.defn
async def some_activity() -> None:
    return None


@workflow.defn
class HelloWorkflow:
    @workflow.run
    async def run(self, name: str) -> str:
        return f"Hello, {name}!"


@activity.defn
def my_sync_activity(name: str) -> str:
    # A blocking call is fine here because the Activity runs on the executor's thread.
    time.sleep(0.1)
    return f"Hello, {name}!"


@workflow.defn
class MyWorkflow:
    @workflow.run
    async def run(self, name: str) -> str:
        return await workflow.execute_activity(
            my_sync_activity, name, start_to_close_timeout=timedelta(seconds=10)
        )

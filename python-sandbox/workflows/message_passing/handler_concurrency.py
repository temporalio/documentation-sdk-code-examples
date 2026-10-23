# @@@SNIPSTART python-message-passing-handler-concurrency
import asyncio
from dataclasses import dataclass
from datetime import timedelta
from typing import Optional

from temporalio import activity, workflow


@dataclass
class Data:
    x: int
    y: int


@activity.defn
async def fetch_data() -> Data:
    return Data(x=1, y=2)


@workflow.defn
class MyWorkflow:
    def __init__(self) -> None:
        self.x: Optional[int] = None
        self.y: Optional[int] = None
        self.lock = asyncio.Lock()

    @workflow.run
    async def run(self) -> list[int]:
        await workflow.wait_condition(lambda: self.y is not None)
        await workflow.wait_condition(workflow.all_handlers_finished)
        return [self.x, self.y]

    @workflow.signal
    async def bad_async_handler(self):
        data = await workflow.execute_activity(
            fetch_data, start_to_close_timeout=timedelta(seconds=10)
        )
        self.x = data.x
        # 🐛🐛 Bug!! If multiple instances of this handler are executing concurrently, then
        # there may be times when the Workflow has self.x from one Activity execution and self.y from another.
        await asyncio.sleep(1)  # or await anything else
        self.y = data.y

    @workflow.signal
    async def safe_async_handler(self):
        async with self.lock:
            data = await workflow.execute_activity(
                fetch_data, start_to_close_timeout=timedelta(seconds=10)
            )
            self.x = data.x
            # ✅ OK: the scheduler may switch now to a different handler execution, or to the main workflow
            # method, but no other execution of this handler can run until this execution finishes.
            await asyncio.sleep(1)  # or await anything else
            self.y = data.y
# @@@SNIPEND

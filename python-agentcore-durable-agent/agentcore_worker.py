import asyncio
import os
from concurrent.futures import ThreadPoolExecutor
from datetime import timedelta

from bedrock_agentcore.runtime import BedrockAgentCoreApp
from temporalio.client import Client
from temporalio.common import VersioningBehavior
from temporalio.contrib.strands import StrandsPlugin
from temporalio.worker import (
    ActivityInboundInterceptor,
    ExecuteActivityInput,
    Interceptor,
    Worker,
    WorkerDeploymentConfig,
    WorkerDeploymentVersion,
)

from activities import execute_code
from workflows import BUILD_ID, DEPLOYMENT_NAME, TASK_QUEUE, DurableAgentWorkflow

app = BedrockAgentCoreApp()
log = app.logger

DEBOUNCE = float(os.environ.get("AGENTCORE_DEBOUNCE_SECONDS", "60"))
DRAIN = timedelta(minutes=2)


def required_env(key: str) -> str:
    value = os.environ.get(key)
    if not value:
        raise RuntimeError(f"{key} is required")
    return value


# @@@SNIPSTART python-agentcore-durable-agent-activity-tracker
class ActivityTracker(Interceptor):
    def __init__(self) -> None:
        self.inflight = 0
        self.changed = asyncio.Event()

    def intercept_activity(
        self, next: ActivityInboundInterceptor
    ) -> ActivityInboundInterceptor:
        return _TrackedActivity(next, self)

    async def wait_until_idle(self, idle_seconds: float) -> None:
        while True:
            self.changed.clear()
            try:
                await asyncio.wait_for(self.changed.wait(), timeout=idle_seconds)
            except asyncio.TimeoutError:
                if self.inflight == 0:
                    return


class _TrackedActivity(ActivityInboundInterceptor):
    def __init__(
        self, next: ActivityInboundInterceptor, tracker: ActivityTracker
    ) -> None:
        super().__init__(next)
        self._tracker = tracker

    async def execute_activity(self, input: ExecuteActivityInput):
        self._tracker.inflight += 1
        self._tracker.changed.set()
        try:
            return await self.next.execute_activity(input)
        finally:
            self._tracker.inflight -= 1
            self._tracker.changed.set()


# @@@SNIPEND


async def run_worker() -> None:
    """Poll until idle, then drain the Worker."""
    client = await Client.connect(
        required_env("TEMPORAL_ADDRESS"),
        namespace=required_env("TEMPORAL_NAMESPACE"),
        api_key=required_env("TEMPORAL_API_KEY"),
        tls=True,
        plugins=[StrandsPlugin()],
    )
    tracker = ActivityTracker()

    with ThreadPoolExecutor(max_workers=4) as activity_executor:
        worker = Worker(
            client,
            task_queue=os.environ.get("TEMPORAL_TASK_QUEUE", TASK_QUEUE),
            workflows=[DurableAgentWorkflow],
            activities=[execute_code],
            activity_executor=activity_executor,
            interceptors=[tracker],
            deployment_config=WorkerDeploymentConfig(
                version=WorkerDeploymentVersion(
                    deployment_name=os.environ.get(
                        "TEMPORAL_DEPLOYMENT_NAME", DEPLOYMENT_NAME
                    ),
                    build_id=os.environ.get("TEMPORAL_BUILD_ID", BUILD_ID),
                ),
                use_worker_versioning=True,
                default_versioning_behavior=VersioningBehavior.PINNED,
            ),
            graceful_shutdown_timeout=DRAIN,
        )
        async with worker:
            await tracker.wait_until_idle(DEBOUNCE)

    log.info("worker idle for %ss and drained", DEBOUNCE)


# @@@SNIPSTART python-agentcore-durable-agent-runtime-handler
_worker: asyncio.Task[None] | None = None


async def _run_until_idle(task_id: int) -> None:
    try:
        await run_worker()
    except Exception:
        log.exception("worker failed in background task")
    finally:
        app.complete_async_task(task_id)


@app.entrypoint
async def invoke(payload: dict) -> dict:
    global _worker
    if _worker is not None and not _worker.done():
        return {"message": "Worker already polling", "task_queue": TASK_QUEUE}

    task_id = app.add_async_task("temporal-worker")
    _worker = asyncio.create_task(_run_until_idle(task_id))

    return {"message": "Worker starting", "task_queue": TASK_QUEUE}


# @@@SNIPEND


if __name__ == "__main__":
    app.run()

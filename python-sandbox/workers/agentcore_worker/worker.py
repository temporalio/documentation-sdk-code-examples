"""Runs a Temporal Worker inside an Amazon Bedrock AgentCore Runtime.

``BedrockAgentCoreApp`` serves /ping and /invocations, and reports "HealthyBusy"
while an async task is registered. The Worker starts after an invocation, runs in
the background, and drains once it has been idle for AGENTCORE_DEBOUNCE_SECONDS.
"""

import asyncio
import os
from datetime import timedelta

from bedrock_agentcore.runtime import BedrockAgentCoreApp
from temporalio.client import Client
from temporalio.common import VersioningBehavior, WorkerDeploymentVersion
from temporalio.worker import (
    ActivityInboundInterceptor,
    ExecuteActivityInput,
    Interceptor,
    Worker,
    WorkerDeploymentConfig,
)

from agentcore_worker import workflows
from agentcore_worker.activities import my_activity

app = BedrockAgentCoreApp()
log = app.logger

# The background Worker, when one is running. Holds the strong reference that
# asyncio.create_task() does not keep, and doubles as the "already polling?" flag.
_worker: asyncio.Task[None] | None = None

DEPLOYMENT_NAME = os.environ.get("TEMPORAL_DEPLOYMENT_NAME", workflows.DEPLOYMENT_NAME)
BUILD_ID = os.environ.get("TEMPORAL_BUILD_ID", workflows.BUILD_ID)
TASK_QUEUE = os.environ.get("TEMPORAL_TASK_QUEUE", workflows.TASK_QUEUE)

# @@@SNIPSTART python-agentcore-sandbox-activity-tracker
# How long the Worker keeps polling after it goes idle.
DEBOUNCE = float(os.environ.get("AGENTCORE_DEBOUNCE_SECONDS", "60"))
# How long the drain waits for in-flight Activities.
DRAIN = timedelta(seconds=120)


class ActivityTracker(Interceptor):
    """Tracks in-flight activities and blocks until AGENTCORE_DEBOUNCE_SECONDS elapses with no events."""

    def __init__(self) -> None:
        self.inflight = 0
        self.changed = asyncio.Event()

    def intercept_activity(
        self, next: ActivityInboundInterceptor
    ) -> ActivityInboundInterceptor:
        return _TrackedActivity(next, self)

    async def wait_until_idle(self, debounce: float) -> None:
        """Return once no Activity has run for ``debounce`` seconds."""
        while True:
            self.changed.clear()
            try:
                # Wake the moment an Activity starts or finishes; a timeout
                # instead means nothing has happened for the whole window.
                await asyncio.wait_for(self.changed.wait(), timeout=debounce)
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
        log.info("activity in flight: %d", self._tracker.inflight)
        try:
            return await self.next.execute_activity(input)
        finally:
            self._tracker.inflight -= 1
            self._tracker.changed.set()
# @@@SNIPEND


# @@@SNIPSTART python-agentcore-sandbox-runtime-handler
async def run_worker() -> None:
    """Poll until idle, then drain."""
    api_key = os.environ.get("TEMPORAL_API_KEY") or None
    client = await Client.connect(
        os.environ.get("TEMPORAL_ADDRESS", "localhost:7233"),
        namespace=os.environ.get("TEMPORAL_NAMESPACE", "default"),
        api_key=api_key,
        tls=bool(api_key),
    )

    tracker = ActivityTracker()
    log.info("polling %s as %s/%s", TASK_QUEUE, DEPLOYMENT_NAME, BUILD_ID)
    worker = Worker(
        client,
        task_queue=TASK_QUEUE,
        workflows=[workflows.ProcessItemsWorkflow],
        activities=[my_activity],
        interceptors=[tracker],
        deployment_config=WorkerDeploymentConfig(
            version=WorkerDeploymentVersion(
                deployment_name=DEPLOYMENT_NAME,
                build_id=BUILD_ID,
            ),
            use_worker_versioning=True,
            default_versioning_behavior=VersioningBehavior.PINNED,
        ),
        graceful_shutdown_timeout=DRAIN,
    )
    async with worker:
        await tracker.wait_until_idle(DEBOUNCE)
    log.info("worker idle for %ss; drained", DEBOUNCE)


async def _run_until_idle(task_id: int) -> None:
    """Own the Worker's whole life, and always release the async task."""
    try:
        await run_worker()
    except Exception:
        # Nothing awaits this task, so an error would otherwise be swallowed.
        log.exception("worker failed in async task")
    finally:
        # Without this the session stays HealthyBusy until MaxLifetime.
        app.complete_async_task(task_id)


@app.entrypoint
async def invoke(payload: dict) -> dict:
    """Start the Worker and acknowledge. The payload is unused."""
    # Prevent duplicate workers since we exit early
    global _worker
    if _worker is not None and not _worker.done():
        log.info("worker already polling %s", TASK_QUEUE)
        return {"message": "worker already polling", "task_queue": TASK_QUEUE}

    task_id = app.add_async_task("temporal-worker")
    _worker = asyncio.create_task(_run_until_idle(task_id))

    return {"message": "worker starting", "task_queue": TASK_QUEUE}
# @@@SNIPEND


if __name__ == "__main__":
    app.run()

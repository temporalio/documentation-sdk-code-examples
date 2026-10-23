import asyncio
import dataclasses
import uuid

import pytest

pytest.importorskip("bedrock_agentcore")

from temporalio.api.enums.v1 import TaskQueueType
from temporalio.api.taskqueue.v1 import TaskQueue
from temporalio.api.workflowservice.v1 import (
    DescribeTaskQueueRequest,
    SetWorkerDeploymentCurrentVersionRequest,
)
from temporalio.client import Client
from temporalio.testing import ActivityEnvironment, WorkflowEnvironment

from agentcore_worker import worker as agentcore
from agentcore_worker.activities import my_activity
from agentcore_worker.workflows import ProcessItemsWorkflow


@pytest.mark.asyncio
async def test_heartbeat_activity_resumes_from_last_heartbeat():
    heartbeats = []
    env = ActivityEnvironment()
    env.on_heartbeat = lambda *details: heartbeats.append(details[0])
    assert await env.run(my_activity, ["a", "b", "c"]) == "done"
    assert heartbeats == [0, 1, 2]

    # A retry that recorded index 1 resumes at index 2.
    heartbeats.clear()
    env.info = dataclasses.replace(env.info, heartbeat_details=[1])
    await env.run(my_activity, ["a", "b", "c"])
    assert heartbeats == [2]


@pytest.mark.asyncio
async def test_activity_tracker_waits_until_idle():
    tracker = agentcore.ActivityTracker()
    tracker.inflight = 1

    async def finish_activity():
        await asyncio.sleep(0.2)
        tracker.inflight = 0
        tracker.changed.set()

    finisher = asyncio.create_task(finish_activity())
    await asyncio.wait_for(tracker.wait_until_idle(0.5), timeout=5)
    await finisher
    assert tracker.inflight == 0


@pytest.mark.asyncio
async def test_invoke_starts_one_worker_that_drains_when_idle(monkeypatch):
    async with await WorkflowEnvironment.start_local() as env:
        real_connect = Client.connect
        target = env.client.service_client.config.target_host

        async def connect(_target, **kwargs):
            return await real_connect(target, **kwargs)

        monkeypatch.setattr(Client, "connect", connect)
        monkeypatch.setattr(agentcore, "DEBOUNCE", 2.0)
        monkeypatch.setattr(agentcore, "_worker", None)

        assert (await agentcore.invoke({}))["message"] == "worker starting"
        assert (await agentcore.invoke({}))["message"] == "worker already polling"

        # Wait for the versioned Worker to poll, then make its version current
        # so the Workflow routes to it.
        request = DescribeTaskQueueRequest(
            namespace=env.client.namespace,
            task_queue=TaskQueue(name=agentcore.TASK_QUEUE),
            task_queue_type=TaskQueueType.TASK_QUEUE_TYPE_WORKFLOW,
        )
        for _ in range(50):
            if (await env.client.workflow_service.describe_task_queue(request)).pollers:
                break
            await asyncio.sleep(0.2)
        await env.client.workflow_service.set_worker_deployment_current_version(
            SetWorkerDeploymentCurrentVersionRequest(
                namespace=env.client.namespace,
                deployment_name=agentcore.DEPLOYMENT_NAME,
                build_id=agentcore.BUILD_ID,
            )
        )

        result = await env.client.execute_workflow(
            ProcessItemsWorkflow.run,
            ["a", "b"],
            id=str(uuid.uuid4()),
            task_queue=agentcore.TASK_QUEUE,
        )
        assert result == "done"

        # With no more Activities, the Worker drains and releases the async task.
        await asyncio.wait_for(agentcore._worker, timeout=30)
        assert agentcore.app.get_async_task_info()["active_count"] == 0

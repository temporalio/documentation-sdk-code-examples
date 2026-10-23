import asyncio
import uuid

import pytest
from temporalio.api.enums.v1 import TaskQueueType
from temporalio.api.taskqueue.v1 import TaskQueue
from temporalio.api.workflowservice.v1 import DescribeTaskQueueRequest
from temporalio.client import Client
from temporalio.testing import WorkflowEnvironment

from run_worker import create_worker, graceful_shutdown, sync_activity_worker
from run_worker.hello import HelloWorkflow, MyWorkflow
from run_worker.versioned_worker import create_versioned_worker


def patch_connect(monkeypatch, env):
    async def connect(*args, **kwargs):
        return env.client

    monkeypatch.setattr(Client, "connect", connect)


async def run_until_workflow_completes(env, worker_main, workflow_run, expected):
    worker_task = asyncio.create_task(worker_main())
    try:
        result = await env.client.execute_workflow(
            workflow_run, "Temporal", id=str(uuid.uuid4()), task_queue="my-task-queue"
        )
        assert result == expected
    finally:
        worker_task.cancel()
        with pytest.raises(asyncio.CancelledError):
            await worker_task


@pytest.mark.asyncio
async def test_create_worker(monkeypatch):
    async with await WorkflowEnvironment.start_local() as env:
        patch_connect(monkeypatch, env)
        await run_until_workflow_completes(
            env, create_worker.main, HelloWorkflow.run, "Hello, Temporal!"
        )


@pytest.mark.asyncio
async def test_sync_activity_worker(monkeypatch):
    async with await WorkflowEnvironment.start_local() as env:
        patch_connect(monkeypatch, env)
        await run_until_workflow_completes(
            env, sync_activity_worker.main, MyWorkflow.run, "Hello, Temporal!"
        )


@pytest.mark.asyncio
async def test_versioned_worker_polls_with_its_deployment_version():
    async with await WorkflowEnvironment.start_local() as env:
        async with create_versioned_worker(env.client):
            request = DescribeTaskQueueRequest(
                namespace=env.client.namespace,
                task_queue=TaskQueue(name="my-task-queue"),
                task_queue_type=TaskQueueType.TASK_QUEUE_TYPE_WORKFLOW,
            )
            for _ in range(50):
                pollers = (await env.client.workflow_service.describe_task_queue(request)).pollers
                if pollers:
                    break
                await asyncio.sleep(0.2)
            options = pollers[0].deployment_options
            assert options.deployment_name == "my-app"
            assert options.build_id == "1.0"


@pytest.mark.asyncio
async def test_graceful_shutdown_on_interrupt_event(monkeypatch):
    async with await WorkflowEnvironment.start_local() as env:
        patch_connect(monkeypatch, env)
        worker_task = asyncio.create_task(graceful_shutdown.main())
        result = await env.client.execute_workflow(
            HelloWorkflow.run, "Temporal", id=str(uuid.uuid4()), task_queue="my-task-queue"
        )
        assert result == "Hello, Temporal!"
        # Setting the event exits the `async with` block, which shuts the Worker down.
        graceful_shutdown.interrupt_event.set()
        await asyncio.wait_for(worker_task, timeout=30)

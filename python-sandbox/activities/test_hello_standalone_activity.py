import asyncio
from concurrent.futures import ThreadPoolExecutor

import pytest
from temporalio.client import Client
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from hello_standalone_activity import execute_activity, worker
from hello_standalone_activity.my_activity import compose_greeting


def patch_connect(monkeypatch, env):
    async def connect(*args, **kwargs):
        return env.client

    monkeypatch.setattr(Client, "connect", connect)


@pytest.mark.asyncio
async def test_execute_standalone_activity(monkeypatch, capsys):
    async with await WorkflowEnvironment.start_local() as env:
        patch_connect(monkeypatch, env)
        async with Worker(
            env.client,
            task_queue="my-standalone-activity-task-queue",
            activities=[compose_greeting],
            activity_executor=ThreadPoolExecutor(5),
        ):
            await execute_activity.my_application()
        assert "Activity result: Hello, World!" in capsys.readouterr().out


@pytest.mark.asyncio
async def test_worker_runs_standalone_activity(monkeypatch, capsys):
    async with await WorkflowEnvironment.start_local() as env:
        patch_connect(monkeypatch, env)
        worker_task = asyncio.create_task(worker.main())
        try:
            await execute_activity.my_application()
        finally:
            worker_task.cancel()
            with pytest.raises(asyncio.CancelledError):
                await worker_task
        out = capsys.readouterr().out
        assert "worker running..." in out
        assert "Activity result: Hello, World!" in out

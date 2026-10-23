import asyncio
from concurrent.futures import ThreadPoolExecutor

import pytest
from temporalio.client import Client
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from hello_standalone_activity import (
    count_activities,
    execute_activity,
    get_activity_handle,
    list_activities,
    start_activity,
    worker,
)
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


@pytest.mark.asyncio
async def test_start_get_list_and_count(monkeypatch, capsys):
    async with await WorkflowEnvironment.start_local() as env:
        patch_connect(monkeypatch, env)
        async with Worker(
            env.client,
            task_queue="my-standalone-activity-task-queue",
            activities=[compose_greeting],
            activity_executor=ThreadPoolExecutor(5),
        ):
            await start_activity.my_application()
            out = capsys.readouterr().out
            assert "Activity result: Hello, World!" in out
            run_id = out.split("Run ID: ")[1].split()[0]

            await get_activity_handle.my_application(run_id)
            assert "Activity result: Hello, World!" in capsys.readouterr().out

        # Visibility is eventually consistent, so wait for the Activity to show up.
        for _ in range(50):
            await list_activities.my_application()
            out = capsys.readouterr().out
            if "ActivityID: my-standalone-activity-id" in out:
                break
            await asyncio.sleep(0.2)
        assert "Type: compose_greeting" in out

        await count_activities.my_application()
        assert "Total activities: 1" in capsys.readouterr().out

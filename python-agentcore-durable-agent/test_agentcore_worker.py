import asyncio

import pytest

import agentcore_worker


@pytest.mark.asyncio
async def test_invoke_starts_one_background_worker(monkeypatch) -> None:
    started = asyncio.Event()
    release = asyncio.Event()
    completed: list[int] = []

    async def run_worker() -> None:
        started.set()
        await release.wait()

    monkeypatch.setattr(agentcore_worker, "run_worker", run_worker)
    monkeypatch.setattr(agentcore_worker.app, "add_async_task", lambda _: 42)
    monkeypatch.setattr(
        agentcore_worker.app,
        "complete_async_task",
        lambda task_id: completed.append(task_id),
    )
    monkeypatch.setattr(agentcore_worker, "_worker", None)

    result = await agentcore_worker.invoke({})
    await started.wait()

    assert result == {"message": "Worker starting", "task_queue": "durable-agent"}
    assert agentcore_worker._worker is not None
    assert not agentcore_worker._worker.done()

    duplicate = await agentcore_worker.invoke({})
    assert duplicate == {
        "message": "Worker already polling",
        "task_queue": "durable-agent",
    }

    release.set()
    await agentcore_worker._worker
    assert completed == [42]

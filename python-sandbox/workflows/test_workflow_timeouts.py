import pytest
from temporalio.client import Client
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from workflow_timeouts import starter
from workflow_timeouts.your_workflows import YourWorkflow


@pytest.mark.asyncio
async def test_starter_sets_timeout_and_retry_policy(monkeypatch, capsys):
    async with await WorkflowEnvironment.start_local() as env:

        async def connect(*args, **kwargs):
            return env.client

        # Point the starter's Client.connect at the test server.
        monkeypatch.setattr(Client, "connect", connect)
        async with Worker(
            env.client,
            task_queue="your-task-queue",
            workflows=[YourWorkflow],
        ):
            await starter.main()

        out = capsys.readouterr().out
        assert "Result: Hello, your timeout argument!" in out
        assert "Handle: your-workflow-id" in out

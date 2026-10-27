import asyncio
import datetime
import uuid

import pytest
import pytest_asyncio
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from sdk_sandbox import notification_workflow, workers
from sdk_sandbox.passthrough_workflow import MyWorkflow
from sdk_sandbox.unrestricted_block import UnrestrictedBlockWorkflow
from sdk_sandbox.unsandboxed_workflow import UnsandboxedWorkflow

pytestmark = pytest.mark.asyncio
TODAY = datetime.date.today().isoformat()


@pytest_asyncio.fixture
async def env():
    async with await WorkflowEnvironment.start_local() as env:
        yield env


async def execute(env, worker, workflow_run, *args):
    async with worker:
        return await env.client.execute_workflow(
            workflow_run, *args, id=str(uuid.uuid4()), task_queue=worker.task_queue
        )


def plain_worker(env, workflows):
    return Worker(env.client, task_queue=str(uuid.uuid4()), workflows=workflows)


async def test_unrestricted_block_and_unsandboxed_workflow(env):
    assert await execute(env, plain_worker(env, [UnrestrictedBlockWorkflow]), UnrestrictedBlockWorkflow.run) == TODAY
    assert await execute(env, plain_worker(env, [UnsandboxedWorkflow]), UnsandboxedWorkflow.run) == TODAY


async def test_passthrough_workflow_and_worker(env):
    worker = workers.passthrough_worker(env.client, str(uuid.uuid4()))
    assert await execute(env, worker, MyWorkflow.run, "Temporal") == "Hello, Temporal!"


async def test_notification_policy_workflow_and_worker(env):
    worker = Worker(
        env.client, task_queue=str(uuid.uuid4()), workflows=[notification_workflow.MyWorkflow]
    )
    assert await execute(env, worker, notification_workflow.MyWorkflow.run, "Temporal") == "Hello, Temporal!"
    # WARN_ON_UNINTENTIONAL_PASSTHROUGH warns about modules that weren't passed through.
    worker = workers.notification_policy_worker(env.client, str(uuid.uuid4()))
    with pytest.warns(UserWarning, match="was not intentionally passed through"):
        assert await execute(env, worker, MyWorkflow.run, "Temporal") == "Hello, Temporal!"


async def test_unrestricting_a_member(env):
    worker = workers.unrestricted_today_worker(env.client, str(uuid.uuid4()))
    assert await execute(env, worker, workers.TodayWorkflow.run) == TODAY


async def test_restricting_a_member(env):
    # With datetime.date restricted, the Workflow Task fails instead of completing.
    worker = workers.restricted_date_worker(env.client, str(uuid.uuid4()))
    async with worker:
        handle = await env.client.start_workflow(
            workers.TodayWorkflow.run, id=str(uuid.uuid4()), task_queue=worker.task_queue
        )
        for _ in range(50):
            events = (await handle.fetch_history()).events
            if any(e.HasField("workflow_task_failed_event_attributes") for e in events):
                break
            await asyncio.sleep(0.2)
        failure = next(
            e.workflow_task_failed_event_attributes.failure.message
            for e in events
            if e.HasField("workflow_task_failed_event_attributes")
        )
        assert "datetime.date" in failure
        await handle.terminate()

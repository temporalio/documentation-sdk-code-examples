import uuid

import pytest
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from timers.looping_workflow import LoopingWorkflow


@pytest.mark.asyncio
async def test_looping_workflow_sleeps_then_continues_as_new():
    task_queue_name = str(uuid.uuid4())
    async with await WorkflowEnvironment.start_time_skipping() as env:
        async with Worker(
            env.client,
            task_queue=task_queue_name,
            workflows=[LoopingWorkflow],
        ):
            handle = await env.client.start_workflow(
                LoopingWorkflow.run,
                0,
                id=str(uuid.uuid4()),
                task_queue=task_queue_name,
            )
            # The result follows the Continue-As-New chain to the final run.
            assert await handle.result() is None

            first_run = env.client.get_workflow_handle(
                handle.id, run_id=handle.first_execution_run_id
            )
            history = await first_run.fetch_history()
            assert any(e.HasField("timer_started_event_attributes") for e in history.events)
            latest = await env.client.get_workflow_handle(handle.id).describe()
            assert latest.run_id != handle.first_execution_run_id

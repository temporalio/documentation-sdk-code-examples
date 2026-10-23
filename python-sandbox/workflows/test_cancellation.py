import uuid

import pytest
from temporalio import workflow
from temporalio.client import WorkflowExecutionStatus
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from cancellation.activity_cancellation import GreetingWorkflow, cancellable_activity
from cancellation.client import cancel_workflow, terminate_workflow
from cancellation.dataobject import ComposeArgsInput


@workflow.defn
class WaitForeverWorkflow:
    @workflow.run
    async def run(self) -> None:
        await workflow.wait_condition(lambda: False)


@pytest.mark.asyncio
async def test_workflow_cancels_activity():
    task_queue_name = str(uuid.uuid4())
    async with await WorkflowEnvironment.start_local() as env:
        async with Worker(
            env.client,
            task_queue=task_queue_name,
            workflows=[GreetingWorkflow],
            activities=[cancellable_activity],
        ):
            handle = await env.client.start_workflow(
                GreetingWorkflow.run,
                ComposeArgsInput(1, 2),
                id=str(uuid.uuid4()),
                task_queue=task_queue_name,
            )
            await handle.result()
            history = await handle.fetch_history()
            assert any(
                e.HasField("activity_task_cancel_requested_event_attributes")
                for e in history.events
            )


@pytest.mark.asyncio
@pytest.mark.parametrize(
    "interrupt, expected_status",
    [
        (cancel_workflow, WorkflowExecutionStatus.CANCELED),
        (terminate_workflow, WorkflowExecutionStatus.TERMINATED),
    ],
)
async def test_interrupt_workflow(interrupt, expected_status):
    task_queue_name = str(uuid.uuid4())
    async with await WorkflowEnvironment.start_local() as env:
        async with Worker(
            env.client,
            task_queue=task_queue_name,
            workflows=[WaitForeverWorkflow],
        ):
            handle = await env.client.start_workflow(
                WaitForeverWorkflow.run,
                id=str(uuid.uuid4()),
                task_queue=task_queue_name,
            )
            await interrupt(env.client, handle.id)
            with pytest.raises(Exception):
                await handle.result()
            assert (await handle.describe()).status == expected_status

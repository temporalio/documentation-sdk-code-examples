import uuid

import pytest
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from enriching_ui import (
    activity_summary_workflow,
    details_workflow,
    starter,
    timer_summary_workflow,
)
from enriching_ui.activities import your_activity


def scheduled_event(history, field):
    return next(e for e in history.events if e.HasField(field))


@pytest.mark.asyncio
async def test_static_summary_and_current_details():
    async with await WorkflowEnvironment.start_local() as env:
        async with Worker(
            env.client, task_queue="your-task-queue", workflows=[details_workflow.YourWorkflow]
        ):
            handle = await starter.start_with_summary(env.client)
            assert await handle.result() == "Workflow completed"
            desc = await handle.describe()
            assert await desc.static_summary() == "Order processing for customer #12345"
            assert await desc.static_details() == "Processing premium order with expedited shipping"

            # The same options work with execute_workflow; the first run has completed,
            # so the Workflow ID can be reused.
            assert await starter.execute_with_summary(env.client) == "Workflow completed"


@pytest.mark.asyncio
async def test_activity_summary():
    async with await WorkflowEnvironment.start_local() as env:
        task_queue = str(uuid.uuid4())
        async with Worker(
            env.client,
            task_queue=task_queue,
            workflows=[activity_summary_workflow.YourWorkflow],
            activities=[your_activity],
        ):
            handle = await env.client.start_workflow(
                activity_summary_workflow.YourWorkflow.run,
                "user data",
                id=str(uuid.uuid4()),
                task_queue=task_queue,
            )
            assert await handle.result() == "Processed user data"
            event = scheduled_event(
                await handle.fetch_history(), "activity_task_scheduled_event_attributes"
            )
            summary = env.client.data_converter.payload_converter.from_payload(
                event.user_metadata.summary, str
            )
            assert summary == "Processing user data"


@pytest.mark.asyncio
async def test_timer_summary():
    async with await WorkflowEnvironment.start_time_skipping() as env:
        task_queue = str(uuid.uuid4())
        async with Worker(
            env.client, task_queue=task_queue, workflows=[timer_summary_workflow.YourWorkflow]
        ):
            handle = await env.client.start_workflow(
                timer_summary_workflow.YourWorkflow.run,
                "order",
                id=str(uuid.uuid4()),
                task_queue=task_queue,
            )
            assert await handle.result() == "Timer completed"
            event = scheduled_event(
                await handle.fetch_history(), "timer_started_event_attributes"
            )
            summary = env.client.data_converter.payload_converter.from_payload(
                event.user_metadata.summary, str
            )
            assert summary == "Waiting for payment confirmation"

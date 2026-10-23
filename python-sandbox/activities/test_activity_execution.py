import uuid

import pytest
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from activity_execution.activities import your_activity
from activity_execution.timeout_workflow import YourWorkflow


@pytest.mark.asyncio
async def test_activity_with_start_to_close_timeout():
    task_queue = str(uuid.uuid4())
    async with await WorkflowEnvironment.start_local() as env:
        async with Worker(
            env.client,
            task_queue=task_queue,
            workflows=[YourWorkflow],
            activities=[your_activity],
        ):
            handle = await env.client.start_workflow(
                YourWorkflow.run, "Hello", id=str(uuid.uuid4()), task_queue=task_queue
            )
            assert await handle.result() == ["Hello, Activity Timeout option!"]

            history = await handle.fetch_history()
            scheduled = next(
                e.activity_task_scheduled_event_attributes
                for e in history.events
                if e.HasField("activity_task_scheduled_event_attributes")
            )
            assert scheduled.start_to_close_timeout.seconds == 10

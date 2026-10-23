import uuid
from datetime import timedelta

import pytest
from temporalio import workflow
from temporalio.testing import ActivityEnvironment, WorkflowEnvironment
from temporalio.worker import Worker

from activity_basics.your_activities import your_activity
from activity_basics.your_dataobject import YourParams


@workflow.defn
class CallsActivityByNameWorkflow:
    @workflow.run
    async def run(self, name: str) -> str:
        # Call the Activity by its custom Activity Type to show that the name is used.
        return await workflow.execute_activity(
            "your_activity",
            YourParams("Hello", name),
            start_to_close_timeout=timedelta(seconds=10),
        )


@pytest.mark.asyncio
async def test_your_activity():
    result = await ActivityEnvironment().run(your_activity, YourParams("Hello", "Temporal"))
    assert result == "Hello, Temporal!"


@pytest.mark.asyncio
async def test_custom_activity_type():
    task_queue = str(uuid.uuid4())
    async with await WorkflowEnvironment.start_local() as env:
        async with Worker(
            env.client,
            task_queue=task_queue,
            workflows=[CallsActivityByNameWorkflow],
            activities=[your_activity],
        ):
            result = await env.client.execute_workflow(
                CallsActivityByNameWorkflow.run,
                "Temporal",
                id=str(uuid.uuid4()),
                task_queue=task_queue,
            )
            assert result == "Hello, Temporal!"

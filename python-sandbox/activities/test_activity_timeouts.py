import uuid
from datetime import timedelta

import pytest
from temporalio import workflow
from temporalio.testing import ActivityEnvironment, WorkflowEnvironment
from temporalio.worker import Worker

from activity_timeouts import retry_workflow, timeout_workflow
from activity_timeouts.activities import your_activity
from activity_timeouts.heartbeat_activities import (
    your_activity_definition,
    your_heartbeating_activity,
)
from activity_timeouts.heartbeat_workflow import HeartbeatTimeoutWorkflow
from activity_timeouts.next_retry_delay import MyActivityInput, my_activity


@workflow.defn
class NextRetryDelayWorkflow:
    @workflow.run
    async def run(self, input: MyActivityInput) -> str:
        return await workflow.execute_activity(
            my_activity, input, start_to_close_timeout=timedelta(seconds=10)
        )


async def run_workflow(env, workflow_run, arg, workflows, activities):
    task_queue = str(uuid.uuid4())
    async with Worker(
        env.client, task_queue=task_queue, workflows=workflows, activities=activities
    ):
        handle = await env.client.start_workflow(
            workflow_run, arg, id=str(uuid.uuid4()), task_queue=task_queue
        )
        return handle, await handle.result()


@pytest.mark.asyncio
async def test_timeout_and_retry_workflows():
    async with await WorkflowEnvironment.start_local() as env:
        _, result = await run_workflow(
            env,
            timeout_workflow.YourWorkflow.run,
            "Hello",
            [timeout_workflow.YourWorkflow],
            [your_activity],
        )
        assert result == "Hello, Activity Timeout option!"

        handle, result = await run_workflow(
            env,
            retry_workflow.YourWorkflow.run,
            "Hello",
            [retry_workflow.YourWorkflow],
            [your_activity],
        )
        assert result == "Hello, Retry Policy options!"
        history = await handle.fetch_history()
        scheduled = next(
            e.activity_task_scheduled_event_attributes
            for e in history.events
            if e.HasField("activity_task_scheduled_event_attributes")
        )
        assert scheduled.retry_policy.maximum_attempts == 5


@pytest.mark.asyncio
async def test_next_retry_delay_overrides_retry_policy():
    async with await WorkflowEnvironment.start_local() as env:
        handle, result = await run_workflow(
            env,
            NextRetryDelayWorkflow.run,
            MyActivityInput(succeed_on_attempt=2),
            [NextRetryDelayWorkflow],
            [my_activity],
        )
        assert result == "succeeded on attempt 2"
        desc = await handle.describe()
        # The single retry waited about 3 seconds (3 * attempt 1), not the 1-second default.
        elapsed = desc.close_time - desc.start_time
        assert elapsed >= timedelta(seconds=3)


@pytest.mark.asyncio
async def test_heartbeat_activity():
    heartbeats = []
    env = ActivityEnvironment()
    env.on_heartbeat = lambda *details: heartbeats.append(details)
    assert await env.run(your_activity_definition) == "done"
    assert heartbeats == [("heartbeat details!",)]


@pytest.mark.asyncio
async def test_heartbeat_timeout_workflow():
    async with await WorkflowEnvironment.start_local() as env:
        _, result = await run_workflow(
            env,
            HeartbeatTimeoutWorkflow.run,
            "Temporal",
            [HeartbeatTimeoutWorkflow],
            [your_heartbeating_activity],
        )
        assert result == ["Hello, World!", "Hello, Temporal!"]

import asyncio
import uuid
from datetime import timedelta

import pytest
from temporalio import activity
from temporalio.client import Client, WorkflowHistory
from temporalio.testing import ActivityEnvironment, WorkflowEnvironment
from temporalio.worker import Replayer, Worker

from testing_suite.replay_workflows import MyWorkflowA, MyWorkflowB, MyWorkflowC, YourWorkflow

pytestmark = pytest.mark.asyncio


async def test_activity_heartbeats():
    # @@@SNIPSTART python-testing-heartbeats
    @activity.defn
    async def activity_with_heartbeats(param: str):
        activity.heartbeat(f"param: {param}")
        activity.heartbeat("second heartbeat")

    env = ActivityEnvironment()
    heartbeats = []
    # Set the `on_heartbeat` property to a callback function that will be called for each Heartbeat sent by the Activity.
    env.on_heartbeat = lambda *args: heartbeats.append(args[0])
    # Use the run method to start the Activity, passing in the function that contains the Heartbeats and any necessary parameters.
    await env.run(activity_with_heartbeats, "test")
    # Verify that the expected Heartbeats are received by the callback function.
    assert heartbeats == ["param: test", "second heartbeat"]
    # @@@SNIPEND


# @@@SNIPSTART python-testing-manual-time-skipping
async def test_manual_time_skipping():
    async with await WorkflowEnvironment.start_time_skipping() as env:
        start = await env.get_current_time()
        # You can use the env.sleep(seconds) method to manually advance time
        await env.sleep(3) # This will advance time by 3 seconds
        assert await env.get_current_time() - start >= timedelta(seconds=3)
# @@@SNIPEND


async def run_on_task_queue(client: Client, task_queue: str) -> list:
    handles = []
    async with Worker(
        client,
        task_queue=task_queue,
        workflows=[MyWorkflowA, MyWorkflowB, MyWorkflowC, YourWorkflow],
    ):
        for workflow_class in [MyWorkflowA, MyWorkflowB, MyWorkflowC]:
            handle = await client.start_workflow(
                workflow_class.run, "replay", id=str(uuid.uuid4()), task_queue=task_queue
            )
            await handle.result()
            handles.append(handle)
    return handles


async def test_replay_histories_from_server():
    async with await WorkflowEnvironment.start_local() as env:
        client = env.client
        await run_on_task_queue(client, "foo")
        # Visibility is eventually consistent, so wait until all three are listed.
        for _ in range(50):
            if len([w async for w in client.list_workflows("TaskQueue='foo'")]) == 3:
                break
            await asyncio.sleep(0.2)

        # @@@SNIPSTART python-testing-replay-from-server
        workflows = client.list_workflows("TaskQueue='foo' and StartTime > '2022-01-01T12:00:00Z'")
        histories = workflows.map_histories()
        replayer = Replayer(
            workflows=[MyWorkflowA, MyWorkflowB, MyWorkflowC]
        )
        await replayer.replay_workflows(histories)
        # @@@SNIPEND


async def test_replay_history_from_json():
    async with await WorkflowEnvironment.start_local() as env:
        task_queue = str(uuid.uuid4())
        async with Worker(env.client, task_queue=task_queue, workflows=[YourWorkflow]):
            handle = await env.client.start_workflow(
                YourWorkflow.run, "Temporal", id=str(uuid.uuid4()), task_queue=task_queue
            )
            await handle.result()
        workflow_id = handle.id
        history_json_str = (await handle.fetch_history()).to_json()

        # @@@SNIPSTART python-testing-replay-from-json
        replayer = Replayer(workflows=[YourWorkflow])
        await replayer.replay_workflow(WorkflowHistory.from_json(workflow_id, history_json_str))
        # @@@SNIPEND


async def test_replay_fails_for_incompatible_workflow():
    async with await WorkflowEnvironment.start_local() as env:
        task_queue = str(uuid.uuid4())
        async with Worker(env.client, task_queue=task_queue, workflows=[YourWorkflow]):
            handle = await env.client.start_workflow(
                YourWorkflow.run, "Temporal", id=str(uuid.uuid4()), task_queue=task_queue
            )
            await handle.result()
        history = await handle.fetch_history()

    # Replaying with a different Workflow Type raises an error.
    with pytest.raises(Exception):
        await Replayer(workflows=[MyWorkflowA]).replay_workflow(history)

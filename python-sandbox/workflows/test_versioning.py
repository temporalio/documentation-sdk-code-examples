import uuid

import pytest
from temporalio.client import WorkflowHistory
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Replayer, Worker

from versioning import (
    workflow_1_initial,
    workflow_2_desired,
    workflow_3_patched,
    workflow_4_deprecated,
)
from versioning.activities import post_patch_activity, pre_patch_activity
from versioning.pizza_workflows import PizzaWorkflow, PizzaWorkflowV2


async def run_and_get_history(env, workflow_cls) -> WorkflowHistory:
    task_queue = str(uuid.uuid4())
    async with Worker(
        env.client,
        task_queue=task_queue,
        workflows=[workflow_cls],
        activities=[pre_patch_activity, post_patch_activity],
    ):
        handle = await env.client.start_workflow(
            workflow_cls.run, id=str(uuid.uuid4()), task_queue=task_queue
        )
        await handle.result()
        return await handle.fetch_history()


async def replays(workflow_cls, history: WorkflowHistory) -> bool:
    result = await Replayer(workflows=[workflow_cls]).replay_workflow(
        history, raise_on_replay_failure=False
    )
    return result.replay_failure is None


@pytest.mark.asyncio
async def test_patching_steps_are_replay_safe():
    async with await WorkflowEnvironment.start_local() as env:
        initial = await run_and_get_history(env, workflow_1_initial.MyWorkflow)
        patched = await run_and_get_history(env, workflow_3_patched.MyWorkflow)
        deprecated = await run_and_get_history(env, workflow_4_deprecated.MyWorkflow)

    # Deploying the desired code directly breaks Workflows started on the initial code.
    assert not await replays(workflow_2_desired.MyWorkflow, initial)

    # The patched code replays both old and new histories.
    assert await replays(workflow_3_patched.MyWorkflow, initial)
    assert await replays(workflow_3_patched.MyWorkflow, patched)

    # The deprecated patch replays histories from the patched code and its own.
    assert await replays(workflow_4_deprecated.MyWorkflow, patched)
    assert await replays(workflow_4_deprecated.MyWorkflow, deprecated)

    # With the patch removed, histories from the deprecated code still replay.
    assert await replays(workflow_2_desired.MyWorkflow, deprecated)


@pytest.mark.asyncio
async def test_cutover_registers_both_workflow_types():
    async with await WorkflowEnvironment.start_local() as env:
        async with Worker(
            env.client,
            task_queue="your-task-queue",
            workflows=[PizzaWorkflow, PizzaWorkflowV2],
        ):
            assert (
                await env.client.execute_workflow(
                    "PizzaWorkflow", "Ada", id=str(uuid.uuid4()), task_queue="your-task-queue"
                )
                == "Pizza for Ada"
            )
            assert (
                await env.client.execute_workflow(
                    "PizzaWorkflowV2", "Ada", id=str(uuid.uuid4()), task_queue="your-task-queue"
                )
                == "Pizza with extra cheese for Ada"
            )

import uuid

import pytest
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from workflow_basics.deterministic_workflow import DeterministicApisWorkflow
from workflow_basics.workflow_init import (
    MyWorkflowInput,
    WorkflowRunSeesWorkflowInitWorkflow,
)
from workflow_basics.your_activities import your_activity
from workflow_basics.your_workflow import YourWorkflow


@pytest.mark.asyncio
async def test_your_workflow():
    task_queue_name = str(uuid.uuid4())
    async with await WorkflowEnvironment.start_local() as env:
        async with Worker(
            env.client,
            task_queue=task_queue_name,
            workflows=[YourWorkflow],
            activities=[your_activity],
        ):
            result = await env.client.execute_workflow(
                "YourWorkflow",
                "Temporal",
                id=str(uuid.uuid4()),
                task_queue=task_queue_name,
            )
            assert result == "Hello, Temporal!"


@pytest.mark.asyncio
async def test_workflow_init():
    task_queue_name = str(uuid.uuid4())
    async with await WorkflowEnvironment.start_local() as env:
        async with Worker(
            env.client,
            task_queue=task_queue_name,
            workflows=[WorkflowRunSeesWorkflowInitWorkflow],
        ):
            handle = await env.client.start_workflow(
                WorkflowRunSeesWorkflowInitWorkflow.get_greeting,
                MyWorkflowInput(name="Arthur"),
                id=str(uuid.uuid4()),
                task_queue=task_queue_name,
            )
            assert await handle.execute_update(
                WorkflowRunSeesWorkflowInitWorkflow.check_title, "Knight"
            )
            assert await handle.result() == "Hello, Knight Arthur"


@pytest.mark.asyncio
async def test_deterministic_apis_workflow():
    task_queue_name = str(uuid.uuid4())
    async with await WorkflowEnvironment.start_local() as env:
        async with Worker(
            env.client,
            task_queue=task_queue_name,
            workflows=[DeterministicApisWorkflow],
        ):
            result = await env.client.execute_workflow(
                DeterministicApisWorkflow.run,
                "Temporal",
                id=str(uuid.uuid4()),
                task_queue=task_queue_name,
            )
            assert result.startswith("Temporal ")

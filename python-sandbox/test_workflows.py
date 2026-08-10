import uuid

import pytest
from temporalio import activity
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from activities import greet
from workflows import SayHelloWorkflow


@pytest.mark.asyncio
async def test_say_hello_workflow():
    """Execute the workflow end-to-end with its real activity."""
    task_queue_name = str(uuid.uuid4())
    async with await WorkflowEnvironment.start_local(ui=True, ui_port=8233) as env:
        async with Worker(
            env.client,
            task_queue=task_queue_name,
            workflows=[SayHelloWorkflow],
            activities=[greet],
        ):
            result = await env.client.execute_workflow(
                SayHelloWorkflow.run,
                "Temporal",
                id=str(uuid.uuid4()),
                task_queue=task_queue_name,
            )
            assert result == "Hello Temporal"


@pytest.mark.asyncio
async def test_say_hello_workflow_with_mocked_activity():
    """Run the workflow with the greet activity mocked out."""

    @activity.defn(name="greet")
    async def greet_mocked(name: str) -> str:
        return "mocked greeting"

    task_queue_name = str(uuid.uuid4())
    async with await WorkflowEnvironment.start_local() as env:
        async with Worker(
            env.client,
            task_queue=task_queue_name,
            workflows=[SayHelloWorkflow],
            activities=[greet_mocked],
        ):
            result = await env.client.execute_workflow(
                SayHelloWorkflow.run,
                "Temporal",
                id=str(uuid.uuid4()),
                task_queue=task_queue_name,
            )
            assert result == "mocked greeting"

import uuid

import pytest
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from child_workflows.workflows import ComposeGreetingWorkflow, GreetingWorkflow


@pytest.mark.asyncio
async def test_greeting_workflow_executes_child():
    task_queue_name = str(uuid.uuid4())
    async with await WorkflowEnvironment.start_local() as env:
        async with Worker(
            env.client,
            task_queue=task_queue_name,
            workflows=[GreetingWorkflow, ComposeGreetingWorkflow],
        ):
            result = await env.client.execute_workflow(
                GreetingWorkflow.run,
                "Temporal",
                id=str(uuid.uuid4()),
                task_queue=task_queue_name,
            )
            assert result == "Hello, Temporal!"

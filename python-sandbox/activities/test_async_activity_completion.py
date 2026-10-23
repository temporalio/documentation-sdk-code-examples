import asyncio
import uuid

import pytest
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from async_activity_completion.activities import pending_completions, request_approval
from async_activity_completion.external_system import complete_activity
from async_activity_completion.workflow import ApprovalWorkflow


@pytest.mark.asyncio
async def test_external_system_completes_activity():
    task_queue = str(uuid.uuid4())
    async with await WorkflowEnvironment.start_local() as env:
        async with Worker(
            env.client,
            task_queue=task_queue,
            workflows=[ApprovalWorkflow],
            activities=[request_approval],
        ):
            handle = await env.client.start_workflow(
                ApprovalWorkflow.run,
                "expense report",
                id=str(uuid.uuid4()),
                task_queue=task_queue,
            )
            captured_token = await asyncio.wait_for(pending_completions.get(), timeout=10)
            await complete_activity(env.client, captured_token)
            assert await handle.result() == "Completion value."

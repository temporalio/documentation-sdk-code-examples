import uuid

import pytest
from temporalio.exceptions import ApplicationError, ApplicationErrorCategory
from temporalio.testing import ActivityEnvironment, WorkflowEnvironment
from temporalio.worker import Worker

from benign_exceptions.activities import my_activity
from benign_exceptions.workflow import PollExternalServiceWorkflow


@pytest.mark.asyncio
async def test_activity_raises_benign_error():
    with pytest.raises(ApplicationError) as err:
        await ActivityEnvironment().run(my_activity)
    assert err.value.category == ApplicationErrorCategory.BENIGN
    assert err.value.message == "external service isn't ready yet"


@pytest.mark.asyncio
async def test_workflow_retries_benign_errors_until_success():
    task_queue = str(uuid.uuid4())
    async with await WorkflowEnvironment.start_local() as env:
        async with Worker(
            env.client,
            task_queue=task_queue,
            workflows=[PollExternalServiceWorkflow],
            activities=[my_activity],
        ):
            result = await env.client.execute_workflow(
                PollExternalServiceWorkflow.run,
                id=str(uuid.uuid4()),
                task_queue=task_queue,
            )
            assert result == "external service is ready"

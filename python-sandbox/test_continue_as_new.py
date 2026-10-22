import uuid

import pytest
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from continue_as_new.workflow import (
    JOBS_TO_PROCESS,
    ClusterManagerInput,
    ClusterManagerWorkflow,
)


@pytest.mark.asyncio
async def test_cluster_manager_continues_as_new():
    task_queue_name = str(uuid.uuid4())
    async with await WorkflowEnvironment.start_time_skipping() as env:
        async with Worker(
            env.client,
            task_queue=task_queue_name,
            workflows=[ClusterManagerWorkflow],
        ):
            handle = await env.client.start_workflow(
                ClusterManagerWorkflow.run,
                ClusterManagerInput(test_continue_as_new=True),
                id=str(uuid.uuid4()),
                task_queue=task_queue_name,
            )
            result = await handle.result()
            assert result.jobs_processed == JOBS_TO_PROCESS

            # The latest run in the chain has a different Run ID from the first run.
            latest = await env.client.get_workflow_handle(handle.id).describe()
            assert latest.run_id != handle.first_execution_run_id

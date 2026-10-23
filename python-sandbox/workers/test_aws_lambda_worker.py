import asyncio
import time
import uuid
from types import SimpleNamespace

import pytest

pytest.importorskip("opentelemetry")

from temporalio.api.enums.v1 import TaskQueueType
from temporalio.api.taskqueue.v1 import TaskQueue
from temporalio.api.workflowservice.v1 import (
    DescribeTaskQueueRequest,
    SetWorkerDeploymentCurrentVersionRequest,
)
from temporalio.testing import WorkflowEnvironment

from aws_lambda_worker.workflows import TASK_QUEUE, SampleWorkflow


@pytest.mark.asyncio
async def test_lambda_handler_runs_versioned_worker(monkeypatch):
    async with await WorkflowEnvironment.start_local() as env:
        monkeypatch.setenv("TEMPORAL_ADDRESS", env.client.service_client.config.target_host)
        monkeypatch.setenv("TEMPORAL_NAMESPACE", env.client.namespace)
        from aws_lambda_worker.lambda_function import lambda_handler

        # A Lambda context with 15 seconds left; the Worker stops 7 seconds early.
        deadline = time.monotonic() + 15
        context = SimpleNamespace(
            get_remaining_time_in_millis=lambda: int((deadline - time.monotonic()) * 1000),
            aws_request_id=str(uuid.uuid4()),
            function_name="test-function",
        )
        # The handler runs its own event loop, so run it on a thread.
        handler = asyncio.create_task(asyncio.to_thread(lambda_handler, {}, context))

        request = DescribeTaskQueueRequest(
            namespace=env.client.namespace,
            task_queue=TaskQueue(name=TASK_QUEUE),
            task_queue_type=TaskQueueType.TASK_QUEUE_TYPE_WORKFLOW,
        )
        for _ in range(50):
            if (await env.client.workflow_service.describe_task_queue(request)).pollers:
                break
            await asyncio.sleep(0.2)
        await env.client.workflow_service.set_worker_deployment_current_version(
            SetWorkerDeploymentCurrentVersionRequest(
                namespace=env.client.namespace, deployment_name="my-app", build_id="build-1"
            )
        )

        result = await env.client.execute_workflow(
            SampleWorkflow.run, "Temporal", id=str(uuid.uuid4()), task_queue=TASK_QUEUE
        )
        assert result == "Hello, Temporal!"
        await asyncio.wait_for(handler, timeout=30)

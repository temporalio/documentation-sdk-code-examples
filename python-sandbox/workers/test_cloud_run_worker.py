import asyncio
import uuid

import pytest

pytest.importorskip("temporalio.contrib.gcp.cloud_run.id")

from temporalio.api.enums.v1 import TaskQueueType
from temporalio.api.taskqueue.v1 import TaskQueue
from temporalio.api.workflowservice.v1 import (
    DescribeTaskQueueRequest,
    SetWorkerDeploymentCurrentVersionRequest,
)
from temporalio.contrib.gcp.cloud_run import id as cloud_run_id
from temporalio.testing import WorkflowEnvironment

from cloud_run_worker import connect_profile, identity_worker, otel_worker, versioned_worker
from cloud_run_worker.greeting_workflow import GreetingWorkflow
from cloud_run_worker.my_workflows import MyWorkflow


async def wait_for_poller(env, task_queue):
    request = DescribeTaskQueueRequest(
        namespace=env.client.namespace,
        task_queue=TaskQueue(name=task_queue),
        task_queue_type=TaskQueueType.TASK_QUEUE_TYPE_WORKFLOW,
    )
    for _ in range(50):
        pollers = (await env.client.workflow_service.describe_task_queue(request)).pollers
        if pollers:
            return pollers
        await asyncio.sleep(0.2)
    raise AssertionError(f"no pollers on {task_queue}")


async def run_workflow_on(env, worker_main, task_queue, versioned=False):
    workflow_run, expected = (MyWorkflow.run, "done") if versioned else (GreetingWorkflow.run, "Hello, Temporal!")
    worker_task = asyncio.create_task(worker_main())
    try:
        pollers = await wait_for_poller(env, task_queue)
        if versioned:
            await env.client.workflow_service.set_worker_deployment_current_version(
                SetWorkerDeploymentCurrentVersionRequest(
                    namespace=env.client.namespace, deployment_name="my-app", build_id="build-1"
                )
            )
        result = await env.client.execute_workflow(
            workflow_run, "Temporal", id=str(uuid.uuid4()), task_queue=task_queue
        )
        assert result == expected
        return pollers
    finally:
        worker_task.cancel()
        with pytest.raises(asyncio.CancelledError):
            await worker_task


@pytest.fixture
def temporal_env(monkeypatch):
    def configure(env, task_queue):
        monkeypatch.setenv("TEMPORAL_ADDRESS", env.client.service_client.config.target_host)
        monkeypatch.setenv("TEMPORAL_NAMESPACE", env.client.namespace)
        monkeypatch.setenv("TEMPORAL_TASK_QUEUE", task_queue)

    return configure


@pytest.mark.asyncio
async def test_versioned_worker(temporal_env):
    async with await WorkflowEnvironment.start_local() as env:
        task_queue = str(uuid.uuid4())
        temporal_env(env, task_queue)
        pollers = await run_workflow_on(env, versioned_worker.main, task_queue, versioned=True)
        assert pollers[0].deployment_options.build_id == "build-1"


@pytest.mark.asyncio
async def test_connect_with_profile(temporal_env):
    async with await WorkflowEnvironment.start_local() as env:
        temporal_env(env, "unused")
        client = await connect_profile.connect()
        assert client.namespace == env.client.namespace


def fake_cloud_run_metadata(monkeypatch):
    # Outside Cloud Run there's no metadata server, so supply the instance metadata.
    metadata = cloud_run_id.GoogleCloudRunMetadata(
        instance_id="instance-123", name="my-worker-pool", revision="my-worker-pool-00001"
    )
    real_plugin = cloud_run_id.CloudRunIdPlugin
    plugin = lambda: real_plugin(metadata=metadata)
    monkeypatch.setattr(identity_worker, "CloudRunIdPlugin", plugin)
    monkeypatch.setattr(otel_worker, "CloudRunIdPlugin", plugin)


@pytest.mark.asyncio
async def test_identity_worker_uses_cloud_run_identity(temporal_env, monkeypatch):
    fake_cloud_run_metadata(monkeypatch)
    async with await WorkflowEnvironment.start_local() as env:
        task_queue = str(uuid.uuid4())
        temporal_env(env, task_queue)
        pollers = await run_workflow_on(env, identity_worker.main, task_queue)
        assert pollers[0].identity == "instance-123@my-worker-pool-00001"


@pytest.mark.asyncio
async def test_otel_worker_runs_without_collector(temporal_env, monkeypatch):
    pytest.importorskip("opentelemetry.exporter.otlp")
    from opentelemetry import trace
    from opentelemetry.util._once import Once

    # The plugin sets the process-wide tracer provider, which OpenTelemetry allows
    # only once per process. Other tests in this run may already have set it.
    monkeypatch.setattr(trace, "_TRACER_PROVIDER", None)
    monkeypatch.setattr(trace, "_TRACER_PROVIDER_SET_ONCE", Once())
    fake_cloud_run_metadata(monkeypatch)

    async def collector_ready():
        return None

    monkeypatch.setattr(otel_worker, "wait_for_collector", collector_ready)
    async with await WorkflowEnvironment.start_local() as env:
        task_queue = str(uuid.uuid4())
        temporal_env(env, task_queue)
        await run_workflow_on(env, otel_worker.main, task_queue)

import asyncio
import uuid

import pytest
from temporalio.client import Client
from temporalio.runtime import (
    BUFFERED_METRIC_KIND_HISTOGRAM,
    MetricBuffer,
    Runtime,
    TelemetryConfig,
)
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from interceptors import activity_metrics, register
from interceptors.context_propagation import (
    HEADER_KEY,
    ContextPropagationInterceptor,
    user_id,
)
from interceptors.workflows import GreetingWorkflow, compose_greeting


def patch_connect(monkeypatch, client):
    async def connect(*args, **kwargs):
        return client

    monkeypatch.setattr(Client, "connect", connect)


@pytest.mark.asyncio
async def test_register_on_client_with_tracing(monkeypatch):
    pytest.importorskip("opentelemetry")
    from temporalio.contrib.opentelemetry import TracingInterceptor

    real_connect = Client.connect
    captured = {}

    async with await WorkflowEnvironment.start_local() as env:
        target = env.client.service_client.config.target_host

        async def connect(_target, **kwargs):
            # Record what the snippet passes, then connect to the test server.
            captured.update(kwargs)
            return await real_connect(target, **kwargs)

        monkeypatch.setattr(Client, "connect", connect)
        client = await register.connect_with_tracing()
        assert isinstance(captured["interceptors"][0], TracingInterceptor)

        task_queue = str(uuid.uuid4())
        async with Worker(
            client,
            task_queue=task_queue,
            workflows=[GreetingWorkflow],
            activities=[compose_greeting],
        ):
            result = await client.execute_workflow(
                GreetingWorkflow.run, "Temporal", id=str(uuid.uuid4()), task_queue=task_queue
            )
            assert result == "Hello, Temporal!"


@pytest.mark.asyncio
async def test_register_on_worker():
    async with await WorkflowEnvironment.start_local() as env:
        worker = register.create_worker(env.client)
        assert isinstance(worker.config()["interceptors"][0], register.SomeWorkerInterceptor)


@pytest.mark.asyncio
async def test_context_propagation_sets_header():
    async with await WorkflowEnvironment.start_local() as env:
        client = Client(
            env.client.service_client,
            namespace=env.client.namespace,
            interceptors=[ContextPropagationInterceptor()],
        )
        task_queue = str(uuid.uuid4())
        async with Worker(
            client,
            task_queue=task_queue,
            workflows=[GreetingWorkflow],
            activities=[compose_greeting],
        ):
            user_id.set("some-user")
            handle = await client.start_workflow(
                GreetingWorkflow.run, "Temporal", id=str(uuid.uuid4()), task_queue=task_queue
            )
            assert await handle.result() == "Hello, Temporal!"
            history = await handle.fetch_history()
            started = history.events[0].workflow_execution_started_event_attributes
            payload = started.header.fields[HEADER_KEY]
            assert client.data_converter.payload_converter.from_payload(payload, str) == "some-user"


@pytest.mark.asyncio
async def test_activity_metrics_interceptor_records_latency(monkeypatch):
    buffer = MetricBuffer(1000)
    runtime = Runtime(telemetry=TelemetryConfig(metrics=buffer))
    async with await WorkflowEnvironment.start_local(runtime=runtime) as env:
        patch_connect(monkeypatch, env.client)
        worker_task = asyncio.create_task(activity_metrics.main())
        try:
            result = await env.client.execute_workflow(
                GreetingWorkflow.run, "Temporal", id=str(uuid.uuid4()), task_queue="my-task-queue"
            )
            assert result == "Hello, Temporal!"
        finally:
            worker_task.cancel()
            with pytest.raises(asyncio.CancelledError):
                await worker_task
    names = {
        update.metric.name
        for update in buffer.retrieve_updates()
        if update.metric.kind == BUFFERED_METRIC_KIND_HISTOGRAM
    }
    assert "custom_activity_schedule_to_start_latency" in names
    assert "custom_activity_schedule_to_close_latency" in names

import asyncio
import logging
import uuid

import pytest
from temporalio.client import Client
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from observability import (
    list_workflows,
    logging_workflow,
    prometheus,
    tracing_interceptor,
    tracing_plugin,
    typed_search_attributes,
)
from observability.my_activities import my_activity
from observability.search_attributes_workflow import CustomerWorkflow, GreetingWorkflow
from observability.traced_workflow import MyWorkflow

REAL_CONNECT = Client.connect


def redirect_connect(monkeypatch, env, calls=None):
    target = env.client.service_client.config.target_host

    async def connect(target_host, **kwargs):
        if calls is not None:
            calls.append((target_host, dict(kwargs)))
        return await REAL_CONNECT(target, **kwargs)

    monkeypatch.setattr(Client, "connect", connect)


@pytest.mark.asyncio
async def test_prometheus_runtime(monkeypatch):
    calls = []
    async with await WorkflowEnvironment.start_local() as env:
        redirect_connect(monkeypatch, env, calls)
        client = await prometheus.connect_with_prometheus()
        assert calls[0][0] == "my.temporal.host:7233"
        assert client.service_client.config.runtime is calls[0][1]["runtime"]


@pytest.mark.asyncio
async def test_opentelemetry_plugin_traces_workflow(monkeypatch):
    from opentelemetry import trace
    from opentelemetry.sdk.trace.export import SimpleSpanProcessor
    from opentelemetry.sdk.trace.export.in_memory_span_exporter import InMemorySpanExporter
    from opentelemetry.util._once import Once

    # Each test may set the process-wide tracer provider, which OpenTelemetry allows once.
    monkeypatch.setattr(trace, "_TRACER_PROVIDER", None)
    monkeypatch.setattr(trace, "_TRACER_PROVIDER_SET_ONCE", Once())

    async with await WorkflowEnvironment.start_local() as env:
        redirect_connect(monkeypatch, env)
        client = await tracing_plugin.main()
        exporter = InMemorySpanExporter()
        trace.get_tracer_provider().add_span_processor(SimpleSpanProcessor(exporter))

        task_queue = str(uuid.uuid4())
        async with Worker(
            client, task_queue=task_queue, workflows=[MyWorkflow], activities=[my_activity]
        ):
            result = await client.execute_workflow(
                MyWorkflow.run, "Temporal", id=str(uuid.uuid4()), task_queue=task_queue
            )
        assert result == "Hello, Temporal!"
        names = {span.name for span in exporter.get_finished_spans()}
        assert "prepare" in names
        assert any(name.startswith("RunWorkflow") for name in names)


@pytest.mark.asyncio
async def test_tracing_interceptor(monkeypatch):
    from temporalio.contrib.opentelemetry import TracingInterceptor

    calls = []
    async with await WorkflowEnvironment.start_local() as env:
        redirect_connect(monkeypatch, env, calls)
        await tracing_interceptor.main()
        assert isinstance(calls[0][1]["interceptors"][0], TracingInterceptor)


@pytest.mark.asyncio
async def test_workflow_logger(caplog):
    async with await WorkflowEnvironment.start_local() as env:
        task_queue = str(uuid.uuid4())
        with caplog.at_level(logging.INFO, logger="temporalio.workflow"):
            async with Worker(
                env.client, task_queue=task_queue, workflows=[logging_workflow.GreetingWorkflow]
            ):
                handle = await env.client.start_workflow(
                    logging_workflow.GreetingWorkflow.run,
                    "Temporal",
                    id=str(uuid.uuid4()),
                    task_queue=task_queue,
                )
                await handle.result()
                assert await handle.query(logging_workflow.GreetingWorkflow.greeting) == "Hello, Temporal!"
        assert "Workflow input parameter: Temporal" in caplog.text


@pytest.fixture
def search_attribute_server_args():
    return [
        "--search-attribute", "CustomerId=Keyword",
        "--search-attribute", "MiscData=Text",
    ]


@pytest.mark.asyncio
async def test_list_workflows_and_watch_search_attributes(monkeypatch, capsys):
    async with await WorkflowEnvironment.start_local(
        dev_server_extra_args=["--search-attribute", "CustomKeywordField=KeywordList"]
    ) as env:
        redirect_connect(monkeypatch, env)
        async with Worker(
            env.client, task_queue="search-attributes-task-queue", workflows=[GreetingWorkflow]
        ):
            await list_workflows.main()
    out = capsys.readouterr().out
    assert "First search attribute values:  ['old-value']" in out
    assert "Second search attribute values:  ['new-value']" in out
    assert "Workflow: search-attributes-workflow-id" in out


@pytest.mark.asyncio
async def test_typed_search_attributes_upsert_and_remove(search_attribute_server_args):
    from temporalio.common import SearchAttributeKey

    customer_id_key = SearchAttributeKey.for_keyword("CustomerId")
    misc_data_key = SearchAttributeKey.for_text("MiscData")
    async with await WorkflowEnvironment.start_local(
        dev_server_extra_args=search_attribute_server_args
    ) as env:
        async with Worker(
            env.client, task_queue="search-attributes-task-queue", workflows=[CustomerWorkflow]
        ):
            handle = await typed_search_attributes.start_with_search_attributes(env.client)
            for _ in range(50):
                attrs = (await handle.describe()).typed_search_attributes
                if attrs.get(customer_id_key) == "customer_2":
                    break
                await asyncio.sleep(0.1)
            assert attrs.get(customer_id_key) == "customer_2"
            assert attrs.get(misc_data_key) == "customer_1_data"

            await handle.signal(CustomerWorkflow.remove_customer_id)
            await handle.result()
            attrs = (await handle.describe()).typed_search_attributes
            assert attrs.get(customer_id_key) is None

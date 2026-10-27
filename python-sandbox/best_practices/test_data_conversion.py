import datetime
import ipaddress
import uuid

import pytest
from temporalio.client import Client
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from data_conversion import ipv4_encoding_converter, ipv4_json_converter, pydantic_client
from data_conversion.models import (
    IPv4Workflow,
    Order,
    OrderWorkflow,
    SingleIPv4Workflow,
    confirm_order,
)

pytestmark = pytest.mark.asyncio
REAL_CONNECT = Client.connect


async def client_with(env, data_converter) -> Client:
    return await REAL_CONNECT(
        env.client.service_client.config.target_host,
        namespace=env.client.namespace,
        data_converter=data_converter,
    )


async def execute(client, workflow, workflow_run, arg, activities=()):
    task_queue = str(uuid.uuid4())
    async with Worker(client, task_queue=task_queue, workflows=[workflow], activities=list(activities)):
        return await client.execute_workflow(
            workflow_run, arg, id=str(uuid.uuid4()), task_queue=task_queue
        )


async def test_pydantic_data_converter(monkeypatch):
    async with await WorkflowEnvironment.start_local() as env:
        target = env.client.service_client.config.target_host

        async def connect(_target, **kwargs):
            return await REAL_CONNECT(target, **kwargs)

        monkeypatch.setattr(Client, "connect", connect)
        client = await pydantic_client.connect()
        order = Order(order_id="order-1", placed_at=datetime.datetime(2026, 1, 1, 12), items=["pizza"])
        result = await execute(client, OrderWorkflow, OrderWorkflow.run, order, [confirm_order])
        assert isinstance(result, Order)
        assert result.items == ["pizza", "receipt"]
        assert result.placed_at == order.placed_at


async def test_encoding_payload_converter_handles_single_values():
    async with await WorkflowEnvironment.start_local() as env:
        client = await client_with(env, ipv4_encoding_converter.my_data_converter)
        result = await execute(
            client, SingleIPv4Workflow, SingleIPv4Workflow.run, ipaddress.IPv4Address("10.0.0.1")
        )
        assert result == ipaddress.IPv4Address("10.0.0.2")


async def test_json_converter_handles_collections():
    async with await WorkflowEnvironment.start_local() as env:
        client = await client_with(env, ipv4_json_converter.my_data_converter)
        addresses = [ipaddress.IPv4Address("10.0.0.1"), ipaddress.IPv4Address("10.0.0.9")]
        result = await execute(client, IPv4Workflow, IPv4Workflow.run, addresses)
        assert result == ipaddress.IPv4Address("10.0.0.9")

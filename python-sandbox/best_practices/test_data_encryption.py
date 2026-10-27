import uuid

import pytest

pytest.importorskip("cramjam")

from temporalio.client import Client
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from data_encryption import client as codec_client
from data_encryption.workflow import EchoWorkflow

REAL_CONNECT = Client.connect


@pytest.mark.asyncio
async def test_codec_compresses_payloads_in_history(monkeypatch):
    async with await WorkflowEnvironment.start_local() as env:
        target = env.client.service_client.config.target_host

        async def connect(_target, **kwargs):
            return await REAL_CONNECT(target, **kwargs)

        monkeypatch.setattr(Client, "connect", connect)
        client = await codec_client.connect()

        task_queue = str(uuid.uuid4())
        message = "sensitive " * 50
        async with Worker(client, task_queue=task_queue, workflows=[EchoWorkflow]):
            handle = await client.start_workflow(
                EchoWorkflow.run, message, id=str(uuid.uuid4()), task_queue=task_queue
            )
            # The codec decodes results for the Client.
            assert await handle.result() == message

        # The Temporal Service stores the compressed payload, which is smaller than the input.
        started = (await env.client.get_workflow_handle(handle.id).fetch_history()).events[0]
        payload = started.workflow_execution_started_event_attributes.input.payloads[0]
        assert payload.metadata["encoding"] == b"binary/snappy"
        assert len(payload.data) < len(message)

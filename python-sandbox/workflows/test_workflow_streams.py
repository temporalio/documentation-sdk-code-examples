import asyncio
import uuid
from types import SimpleNamespace

import pytest
from temporalio.client import Client
from temporalio.contrib.workflow_streams import WorkflowStreamClient
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from workflow_streams import publish_client, subscriber
from workflow_streams.delta_activities import stream_deltas
from workflow_streams.delta_phases import stream_deltas_in_phases, stream_urgent_delta
from workflow_streams.delta_types import Delta
from workflow_streams.delta_workflow import DeltaWorkflow
from workflow_streams.long_running import (
    ExplicitRolloverWorkflow,
    LongRunningWorkflow,
    WorkflowInput,
)
from workflow_streams.order_activities import (
    charge_payment_activity,
    dispatch_order_activity,
    validate_order_activity,
)
from workflow_streams.order_workflow import OrderInput, OrderWorkflow, ProgressEvent


def patch_connect(monkeypatch, env):
    async def connect(*args, **kwargs):
        return env.client

    monkeypatch.setattr(Client, "connect", connect)


@pytest.mark.asyncio
async def test_order_stream(monkeypatch, capsys):
    async with await WorkflowEnvironment.start_time_skipping() as env:
        patch_connect(monkeypatch, env)
        task_queue = str(uuid.uuid4())
        async with Worker(
            env.client,
            task_queue=task_queue,
            workflows=[OrderWorkflow],
            activities=[
                validate_order_activity,
                charge_payment_activity,
                dispatch_order_activity,
            ],
        ):
            order_id = f"order-{uuid.uuid4()}"
            handle = await env.client.start_workflow(
                OrderWorkflow.run, OrderInput(order_id), id=order_id, task_queue=task_queue
            )
            await subscriber.watch_order(order_id)
            out = capsys.readouterr().out
            assert "[  0%] validating: checking inventory" in out
            assert "[100%] completed: " in out

            # Publish from outside the Workflow while it waits before closing.
            await publish_client.publish_status(order_id)
            async with WorkflowStreamClient.create(env.client, order_id) as client:
                client.topic("progress", type=ProgressEvent).publish(
                    ProgressEvent(message="label printed")
                )

            # The heterogeneous subscriber reads until the Workflow completes.
            topics = asyncio.create_task(subscriber.watch_order_topics(env.client, order_id))
            assert await handle.result() == f"order {order_id} shipped"
            await asyncio.wait_for(topics, timeout=30)
            out = capsys.readouterr().out
            assert "[status] started: " in out
            assert "[progress] label printed" in out


@pytest.mark.asyncio
async def test_activity_publishes_deltas():
    async with await WorkflowEnvironment.start_local() as env:
        task_queue = str(uuid.uuid4())
        async with Worker(
            env.client,
            task_queue=task_queue,
            workflows=[DeltaWorkflow],
            activities=[stream_deltas, stream_deltas_in_phases, stream_urgent_delta],
        ):
            order_id = f"order-{uuid.uuid4()}"
            handle = await env.client.start_workflow(
                DeltaWorkflow.run, order_id, id=order_id, task_queue=task_queue
            )
            stream = WorkflowStreamClient.create(env.client, order_id)
            texts = [
                item.data.text
                async for item in stream.topic("delta", type=Delta).subscribe()
            ]
            assert await handle.result() == "checkpoint-1"
            assert texts[0] == f"{order_id}: first delta"
            assert "on" in texts
            assert texts[-2:] == ["phase one", "phase two after checkpoint-1"]


@pytest.mark.asyncio
async def test_long_running_workflow_streams_progress():
    async with await WorkflowEnvironment.start_local() as env:
        task_queue = str(uuid.uuid4())
        async with Worker(env.client, task_queue=task_queue, workflows=[LongRunningWorkflow]):
            workflow_id = str(uuid.uuid4())
            handle = await env.client.start_workflow(
                LongRunningWorkflow.run, WorkflowInput(), id=workflow_id, task_queue=task_queue
            )
            stream = WorkflowStreamClient.create(env.client, workflow_id)
            async for item in stream.topic("progress", type=dict).subscribe():
                if item.data["items_processed"] >= 2:
                    break
            await handle.terminate()


@pytest.mark.asyncio
async def test_explicit_continue_as_new_carries_stream_state():
    async with await WorkflowEnvironment.start_local() as env:
        task_queue = str(uuid.uuid4())
        async with Worker(
            env.client, task_queue=task_queue, workflows=[ExplicitRolloverWorkflow]
        ), Worker(env.client, task_queue="other-tq", workflows=[ExplicitRolloverWorkflow]):
            workflow_id = str(uuid.uuid4())
            handle = await env.client.start_workflow(
                ExplicitRolloverWorkflow.run,
                WorkflowInput(),
                id=workflow_id,
                task_queue=task_queue,
            )
            items = []
            stream = WorkflowStreamClient.create(env.client, workflow_id)
            async for item in stream.topic("progress", type=dict).subscribe():
                items.append(item.data["items_processed"])
                if len(items) == 2:
                    break
            # Both runs' events are in the stream, so subscribers see no gap.
            assert items == [1, 2]
            assert await handle.result() == 2


@pytest.mark.asyncio
async def test_chat_stream_resets_on_retry(monkeypatch):
    pytest.importorskip("openai")
    from workflow_streams.chat import activity as chat_activity
    from workflow_streams.chat.consumer import stream_chat
    from workflow_streams.chat.workflow import ChatInput, ChatWorkflow

    attempts = []

    def chunk(text):
        return SimpleNamespace(choices=[SimpleNamespace(delta=SimpleNamespace(content=text))])

    class FakeCompletions:
        async def create(self, **kwargs):
            attempts.append(1)
            first_attempt = len(attempts) == 1

            async def gen():
                if first_attempt:
                    yield chunk("Hel")
                    await asyncio.sleep(0.5)
                    raise RuntimeError("model connection dropped")
                yield chunk("Hello")
                yield chunk(", world")

            return gen()

    class FakeAsyncOpenAI:
        def __init__(self, **kwargs):
            self.chat = SimpleNamespace(completions=FakeCompletions())

    monkeypatch.setattr(chat_activity, "AsyncOpenAI", FakeAsyncOpenAI)

    async with await WorkflowEnvironment.start_local() as env:
        task_queue = str(uuid.uuid4())
        async with Worker(
            env.client,
            task_queue=task_queue,
            workflows=[ChatWorkflow],
            activities=[chat_activity.stream_completion],
        ):
            chat_id = str(uuid.uuid4())
            handle = await env.client.start_workflow(
                ChatWorkflow.run, ChatInput(prompt="Say hello"), id=chat_id, task_queue=task_queue
            )
            output = await stream_chat(env.client, chat_id)
            assert output == "Hello, world"
            assert await handle.result() == "Hello, world"
            assert len(attempts) == 2

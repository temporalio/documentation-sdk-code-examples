import uuid

import pytest
import pytest_asyncio
from temporalio.client import Client, WorkflowUpdateFailedError, WorkflowUpdateStage
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from message_passing import client as greeting_client
from message_passing import signal_with_start
from message_passing.async_greeting_workflow import (
    GreetingWorkflow as AsyncGreetingWorkflow,
)
from message_passing.async_greeting_workflow import call_greeting_service
from message_passing.dynamic_activity import GreetingWorkflow as DynamicActivityWorkflow
from message_passing.dynamic_activity import dynamic_greeting
from message_passing.dynamic_handlers import DynamicSignalWorkflow
from message_passing.dynamic_workflow import DynamicWorkflow, default_greeting
from message_passing.external_signal import WorkflowA, WorkflowB
from message_passing.greeting_workflow import GreetingWorkflow, Language
from message_passing.handler_concurrency import MyWorkflow as ConcurrencyWorkflow
from message_passing.handler_concurrency import fetch_data
from message_passing.shopping_cart import ShoppingCartWorkflow, get_price
from message_passing.shopping_cart_starter import handle_add_item_request
from message_passing.wait_conditions import AbandonUpdateWorkflow
from message_passing.wait_conditions import MyWorkflow as WaitWorkflow
from message_passing.wait_conditions import UpdateInput
from message_passing.wf_signal import GreetingWorkflow as SignalGreetingWorkflow
from message_passing.workflow_init import (
    MyWorkflowInput,
    WorkflowRunSeesWorkflowInitWorkflow,
    check_title_validity,
)


@pytest_asyncio.fixture
async def env():
    async with await WorkflowEnvironment.start_local() as env:
        yield env


def patch_connect(monkeypatch, env):
    async def connect(*args, **kwargs):
        return env.client

    monkeypatch.setattr(Client, "connect", connect)


@pytest.mark.asyncio
async def test_greeting_client_sends_messages(env, monkeypatch, capsys):
    patch_connect(monkeypatch, env)
    async with Worker(env.client, task_queue="my-task-queue", workflows=[GreetingWorkflow]):
        await greeting_client.main()
    out = capsys.readouterr().out
    assert "Supported languages: [<Language.CHINESE: 2>, <Language.ENGLISH: 3>]" in out
    assert "Previous language: ENGLISH" in out
    assert "Previous language: CHINESE" in out
    assert "Workflow result: Hello, world" in out


@pytest.mark.asyncio
async def test_update_validator_rejects_unsupported_language(env):
    task_queue = str(uuid.uuid4())
    async with Worker(env.client, task_queue=task_queue, workflows=[GreetingWorkflow]):
        handle = await env.client.start_workflow(
            GreetingWorkflow.run, id=str(uuid.uuid4()), task_queue=task_queue
        )
        with pytest.raises(WorkflowUpdateFailedError):
            await handle.execute_update(GreetingWorkflow.set_language, Language.FRENCH)
        await handle.terminate()


@pytest.mark.asyncio
async def test_external_signal(env):
    task_queue = str(uuid.uuid4())
    async with Worker(env.client, task_queue=task_queue, workflows=[WorkflowA, WorkflowB]):
        handle_a = await env.client.start_workflow(
            WorkflowA.run, id="workflow-a", task_queue=task_queue
        )
        await env.client.execute_workflow(
            WorkflowB.run, id=str(uuid.uuid4()), task_queue=task_queue
        )
        assert await handle_a.result() == "signal argument"


@pytest.mark.asyncio
async def test_signal_with_start(env, monkeypatch, capsys):
    patch_connect(monkeypatch, env)
    async with Worker(env.client, task_queue="signal-tq", workflows=[SignalGreetingWorkflow]):
        await signal_with_start.main()
    assert "Hello, User Signal with Start" in capsys.readouterr().out


@pytest.mark.asyncio
async def test_update_with_start(env):
    async with Worker(
        env.client,
        task_queue="my-task-queue",
        workflows=[ShoppingCartWorkflow],
        activities=[get_price],
    ):
        cart_id = str(uuid.uuid4())
        price, handle = await handle_add_item_request(env.client, cart_id, "sku-123", 2)
        assert str(price) == "11.98"
        price, _ = await handle_add_item_request(env.client, cart_id, "sku-unknown", 1)
        assert price is None
        await handle.signal(ShoppingCartWorkflow.checkout)
        assert len(await handle.result()) == 1


@pytest.mark.asyncio
async def test_async_update_handler(env):
    task_queue = str(uuid.uuid4())
    async with Worker(
        env.client,
        task_queue=task_queue,
        workflows=[AsyncGreetingWorkflow],
        activities=[call_greeting_service],
    ):
        handle = await env.client.start_workflow(
            AsyncGreetingWorkflow.run, id=str(uuid.uuid4()), task_queue=task_queue
        )
        previous = await handle.execute_update(
            AsyncGreetingWorkflow.set_language, Language.FRENCH
        )
        assert previous == Language.ENGLISH
        with pytest.raises(WorkflowUpdateFailedError):
            await handle.execute_update(
                AsyncGreetingWorkflow.set_language, Language.PORTUGUESE
            )
        await handle.signal(AsyncGreetingWorkflow.approve)
        assert await handle.result() == "Bonjour, monde"


@pytest.mark.asyncio
async def test_wait_conditions(env):
    task_queue = str(uuid.uuid4())
    async with Worker(env.client, task_queue=task_queue, workflows=[WaitWorkflow]):
        handle = await env.client.start_workflow(
            WaitWorkflow.run, id=str(uuid.uuid4()), task_queue=task_queue
        )
        update_handle = await handle.start_update(
            WaitWorkflow.my_update,
            UpdateInput("value"),
            wait_for_stage=WorkflowUpdateStage.ACCEPTED,
        )
        await handle.signal(WaitWorkflow.start_processing)
        assert await update_handle.result() == "value"
        assert await handle.result() == "workflow-result"


@pytest.mark.asyncio
async def test_abandoned_update_does_not_block_completion(env):
    task_queue = str(uuid.uuid4())
    async with Worker(env.client, task_queue=task_queue, workflows=[AbandonUpdateWorkflow]):
        handle = await env.client.start_workflow(
            AbandonUpdateWorkflow.run, id=str(uuid.uuid4()), task_queue=task_queue
        )
        await handle.start_update(
            AbandonUpdateWorkflow.my_update,
            wait_for_stage=WorkflowUpdateStage.ACCEPTED,
        )
        await handle.signal(AbandonUpdateWorkflow.finish)
        assert await handle.result() == "workflow-result"


@pytest.mark.asyncio
async def test_workflow_init(env):
    task_queue = str(uuid.uuid4())
    async with Worker(
        env.client,
        task_queue=task_queue,
        workflows=[WorkflowRunSeesWorkflowInitWorkflow],
        activities=[check_title_validity],
    ):
        handle = await env.client.start_workflow(
            WorkflowRunSeesWorkflowInitWorkflow.get_greeting,
            MyWorkflowInput(name="Lancelot"),
            id=str(uuid.uuid4()),
            task_queue=task_queue,
        )
        assert await handle.execute_update(
            WorkflowRunSeesWorkflowInitWorkflow.check_title_validity
        )
        assert await handle.result() == "Hello, Sir Lancelot"


@pytest.mark.asyncio
async def test_handler_concurrency(env):
    task_queue = str(uuid.uuid4())
    async with Worker(
        env.client,
        task_queue=task_queue,
        workflows=[ConcurrencyWorkflow],
        activities=[fetch_data],
    ):
        handle = await env.client.start_workflow(
            ConcurrencyWorkflow.run, id=str(uuid.uuid4()), task_queue=task_queue
        )
        await handle.signal(ConcurrencyWorkflow.safe_async_handler)
        await handle.signal(ConcurrencyWorkflow.safe_async_handler)
        assert await handle.result() == [1, 2]


@pytest.mark.asyncio
async def test_dynamic_signal(env):
    task_queue = str(uuid.uuid4())
    async with Worker(env.client, task_queue=task_queue, workflows=[DynamicSignalWorkflow]):
        handle = await env.client.start_workflow(
            DynamicSignalWorkflow.run, id=str(uuid.uuid4()), task_queue=task_queue
        )
        await handle.signal("some-name", "my_arg")
        assert await handle.result() == ["some-name: my_arg"]


@pytest.mark.asyncio
async def test_dynamic_workflow(env):
    task_queue = str(uuid.uuid4())
    async with Worker(
        env.client,
        task_queue=task_queue,
        workflows=[DynamicWorkflow],
        activities=[default_greeting],
    ):
        result = await env.client.execute_workflow(
            "MyUnknownWorkflow", "Temporal", id=str(uuid.uuid4()), task_queue=task_queue
        )
        assert result.startswith("Hello, Temporal!")


@pytest.mark.asyncio
async def test_dynamic_activity(env):
    task_queue = str(uuid.uuid4())
    async with Worker(
        env.client,
        task_queue=task_queue,
        workflows=[DynamicActivityWorkflow],
        activities=[dynamic_greeting],
    ):
        result = await env.client.execute_workflow(
            DynamicActivityWorkflow.run,
            "Temporal",
            id=str(uuid.uuid4()),
            task_queue=task_queue,
        )
        assert result == "Hello, Temporal!\nActivity Type: unregistered_activity"

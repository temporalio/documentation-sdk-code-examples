import asyncio
import json
import uuid
from collections.abc import AsyncIterable
from typing import Any

from strands.models import Model
from strands.types.streaming import StreamEvent
from temporalio.contrib.strands import StrandsPlugin
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from workflows import MODEL_NAME, DurableAgentWorkflow


class RecordingModel(Model):
    def __init__(self, response: str, received_messages: list[Any]) -> None:
        self.response = response
        self.received_messages = received_messages

    def update_config(self, **_model_config: Any) -> None:
        return None

    def get_config(self) -> dict[str, Any]:
        return {}

    def structured_output(self, *_args: Any, **_kwargs: Any):
        raise NotImplementedError

    async def stream(
        self, messages: Any, *_args: Any, **_kwargs: Any
    ) -> AsyncIterable[StreamEvent]:
        self.received_messages.append(messages)
        yield {"messageStart": {"role": "assistant"}}
        yield {"contentBlockDelta": {"delta": {"text": self.response}}}
        yield {"contentBlockStop": {}}
        yield {"messageStop": {"stopReason": "end_turn"}}


async def test_conversation_survives_worker_replacement() -> None:
    task_queue = str(uuid.uuid4())
    workflow_id = str(uuid.uuid4())
    first_worker_messages: list[Any] = []
    second_worker_messages: list[Any] = []

    async with await WorkflowEnvironment.start_time_skipping() as env:
        first_plugin = StrandsPlugin(
            models={
                MODEL_NAME: lambda: RecordingModel(
                    "There are 28.", first_worker_messages
                )
            }
        )
        async with Worker(
            env.client,
            task_queue=task_queue,
            workflows=[DurableAgentWorkflow],
            plugins=[first_plugin],
            max_cached_workflows=0,
        ):
            handle = await env.client.start_workflow(
                DurableAgentWorkflow.run,
                id=workflow_id,
                task_queue=task_queue,
            )
            first_answer = await asyncio.wait_for(
                handle.execute_update(
                    DurableAgentWorkflow.ask,
                    "What is 7 times 4?",
                ),
                timeout=15,
            )
            assert first_answer == "There are 28."

        second_plugin = StrandsPlugin(
            models={
                MODEL_NAME: lambda: RecordingModel(
                    "Adding two gives 30.", second_worker_messages
                )
            }
        )
        async with Worker(
            env.client,
            task_queue=task_queue,
            workflows=[DurableAgentWorkflow],
            plugins=[second_plugin],
            max_cached_workflows=0,
        ):
            handle = env.client.get_workflow_handle_for(
                DurableAgentWorkflow.run,
                workflow_id,
            )
            second_answer = await asyncio.wait_for(
                handle.execute_update(
                    DurableAgentWorkflow.ask,
                    "Add two to your previous answer.",
                ),
                timeout=15,
            )
            assert second_answer == "Adding two gives 30."
            await handle.signal(DurableAgentWorkflow.finish)
            await asyncio.wait_for(handle.result(), timeout=15)

    serialized_messages = json.dumps(second_worker_messages)
    assert "What is 7 times 4?" in serialized_messages
    assert "There are 28." in serialized_messages

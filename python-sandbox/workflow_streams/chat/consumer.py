# @@@SNIPSTART python-workflow-streams-chat-consumer
from temporalio.client import Client
from temporalio.common import RawValue
from temporalio.contrib.workflow_streams import WorkflowStreamClient

from workflow_streams.chat.activity import TextDelta
from workflow_streams.chat.workflow import ChatWorkflow


async def stream_chat(temporal_client: Client, chat_id: str) -> str:
    # Subscribe-only; no `async with` needed because the flusher only runs for publishers.
    stream = WorkflowStreamClient.create(temporal_client, workflow_id=chat_id)
    converter = temporal_client.data_converter.payload_converter
    output: list[str] = []

    def render() -> None:
        ...  # display the accumulated output (terminal redraw, UI update, etc.)

    async for item in stream.subscribe(
        ["delta", "retry", "close"], result_type=RawValue
    ):
        if item.topic == "retry":
            # Earlier attempt's deltas are stale; drop what we've shown.
            output.clear()
            render()
        elif item.topic == "delta":
            delta = converter.from_payload(item.data.payload, TextDelta)
            output.append(delta.text)
            render()
        elif item.topic == "close":
            # Acknowledge so the Workflow can return without a sleep.
            await temporal_client.get_workflow_handle(chat_id).signal(
                ChatWorkflow.subscriber_acknowledged_terminator
            )
            break

    return "".join(output)
# @@@SNIPEND

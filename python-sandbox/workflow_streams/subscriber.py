from temporalio.client import Client
from temporalio.contrib.workflow_streams import WorkflowStreamClient

from workflow_streams.order_workflow import ProgressEvent, StatusEvent


# @@@SNIPSTART python-workflow-streams-subscribe
async def watch_order(order_id: str) -> None:
    temporal_client = await Client.connect("localhost:7233")
    stream = WorkflowStreamClient.create(temporal_client, workflow_id=order_id)

    status = stream.topic("status", type=StatusEvent)
    async for item in status.subscribe():
        evt = item.data
        print(f"[{evt.progress:3d}%] {evt.state}: {evt.detail}")
        if evt.state == "completed":
            break
# @@@SNIPEND


async def watch_order_topics(temporal_client: Client, order_id: str) -> None:
    stream = WorkflowStreamClient.create(temporal_client, workflow_id=order_id)
    # @@@SNIPSTART python-workflow-streams-heterogeneous
    from temporalio.common import RawValue

    converter = temporal_client.data_converter.payload_converter

    async for item in stream.subscribe(["status", "progress"], result_type=RawValue):
        if item.topic == "status":
            evt = converter.from_payload(item.data.payload, StatusEvent)
            print(f"[status] {evt.state}: {evt.detail}")
        elif item.topic == "progress":
            evt = converter.from_payload(item.data.payload, ProgressEvent)
            print(f"[progress] {evt.message}")
    # @@@SNIPEND

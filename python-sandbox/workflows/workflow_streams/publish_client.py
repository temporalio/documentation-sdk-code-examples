# @@@SNIPSTART python-workflow-streams-publish-client
from datetime import timedelta

from temporalio.client import Client
from temporalio.contrib.workflow_streams import WorkflowStreamClient

from workflow_streams.order_workflow import StatusEvent


async def publish_status(workflow_id: str) -> None:
    temporal_client = await Client.connect("localhost:7233")
    stream_client = WorkflowStreamClient.create(
        temporal_client,
        workflow_id=workflow_id,
        batch_interval=timedelta(milliseconds=200),
    )
    async with stream_client:
        status = stream_client.topic("status", type=StatusEvent)
        status.publish(StatusEvent(state="started"))
        ...
        # Buffer is flushed on context manager exit.
# @@@SNIPEND

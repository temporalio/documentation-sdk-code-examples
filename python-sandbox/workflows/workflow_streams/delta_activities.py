# @@@SNIPSTART python-workflow-streams-activity-publish
from temporalio import activity
from temporalio.contrib.workflow_streams import WorkflowStreamClient

from workflow_streams.delta_types import Delta, generate_deltas


@activity.defn
async def stream_deltas(order_id: str) -> None:
    client = WorkflowStreamClient.from_within_activity()
    async with client:
        deltas = client.topic("delta", type=Delta)
        for delta in generate_deltas(order_id):
            deltas.publish(delta)
            activity.heartbeat()
        # Buffer is flushed on context manager exit.
# @@@SNIPEND

from temporalio import activity
from temporalio.contrib.workflow_streams import WorkflowStreamClient

from workflow_streams.delta_types import (
    Delta,
    first_phase,
    record_phase_one_complete,
    second_phase,
)


@activity.defn
async def stream_deltas_in_phases(order_id: str) -> str:
    client = WorkflowStreamClient.from_within_activity()
    # @@@SNIPSTART python-workflow-streams-flush-barrier
    async with client:
        deltas = client.topic("delta", type=Delta)
        for delta in first_phase():
            deltas.publish(delta)

        await client.flush()
        checkpoint_id = await record_phase_one_complete()  # only safe once phase-one events are durable in the Workflow log

        for delta in second_phase(checkpoint_id):
            deltas.publish(delta)
    # @@@SNIPEND
    return checkpoint_id


@activity.defn
async def stream_urgent_delta(order_id: str) -> None:
    client = WorkflowStreamClient.from_within_activity()
    async with client:
        deltas = client.topic("delta", type=Delta)
        delta = Delta(text=f"{order_id}: first delta")
        # @@@SNIPSTART python-workflow-streams-force-flush
        deltas.publish(delta, force_flush=True)
        # @@@SNIPEND

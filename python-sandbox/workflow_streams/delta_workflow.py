from datetime import timedelta

from temporalio import workflow
from temporalio.contrib.workflow_streams import WorkflowStream

with workflow.unsafe.imports_passed_through():
    from workflow_streams.delta_activities import stream_deltas
    from workflow_streams.delta_phases import (
        stream_deltas_in_phases,
        stream_urgent_delta,
    )


@workflow.defn
class DeltaWorkflow:
    """Hosts the stream that the delta Activities publish to."""

    @workflow.init
    def __init__(self, order_id: str) -> None:
        self.stream = WorkflowStream()

    @workflow.run
    async def run(self, order_id: str) -> str:
        await workflow.execute_activity(
            stream_urgent_delta, order_id, start_to_close_timeout=timedelta(seconds=30)
        )
        await workflow.execute_activity(
            stream_deltas, order_id, start_to_close_timeout=timedelta(seconds=30)
        )
        checkpoint_id = await workflow.execute_activity(
            stream_deltas_in_phases,
            order_id,
            start_to_close_timeout=timedelta(seconds=30),
        )
        # Give subscribers time to read the last events before the Workflow closes.
        await workflow.sleep(timedelta(seconds=2))
        return checkpoint_id

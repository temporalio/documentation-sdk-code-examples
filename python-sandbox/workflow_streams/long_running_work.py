from datetime import timedelta
from typing import Any

from temporalio import workflow


async def do_one_iteration(wf: Any) -> None:
    # Stand-in for a unit of work that publishes its progress to the stream.
    wf.app_state.items_processed += 1
    wf.stream.topic("progress", type=dict).publish(
        {"items_processed": wf.app_state.items_processed}
    )
    await workflow.sleep(timedelta(seconds=1))

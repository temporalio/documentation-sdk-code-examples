# @@@SNIPSTART python-client-sync-background-loop
import asyncio
import threading
from typing import Any, Coroutine, TypeVar

from temporalio.client import Client

from temporal_client.your_workflows import YourWorkflow

T = TypeVar("T")


class TemporalClientThread:
    """Runs a Temporal Client on an event loop in a background thread."""

    def __init__(self, target_host: str) -> None:
        self._loop = asyncio.new_event_loop()
        threading.Thread(target=self._loop.run_forever, daemon=True).start()
        # Connect on the background loop so that the Client belongs to it.
        self.client = self.run(Client.connect(target_host))

    def run(self, coro: Coroutine[Any, Any, T]) -> T:
        """Runs a coroutine on the background loop and waits for its result."""
        return asyncio.run_coroutine_threadsafe(coro, self._loop).result()


def start_from_sync_code(temporal: TemporalClientThread, name: str) -> str:
    handle = temporal.run(
        temporal.client.start_workflow(
            YourWorkflow.run,
            name,
            id=f"your-workflow-{name}",
            task_queue="your-task-queue",
        )
    )
    return handle.id


def main() -> None:
    # Create one TemporalClientThread per process and share it.
    temporal = TemporalClientThread("localhost:7233")
    for name in ["alice", "bob"]:
        workflow_id = start_from_sync_code(temporal, name)
        print(f"Started Workflow {workflow_id}")
# @@@SNIPEND


if __name__ == "__main__":
    main()

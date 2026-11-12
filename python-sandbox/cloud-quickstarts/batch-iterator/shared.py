import os
from dataclasses import dataclass

# The Task Queue the Worker polls and the client targets. Keep this in sync
# between the Worker and the client.
TASK_QUEUE = os.getenv("TEMPORAL_TASK_QUEUE", "quickstart-batch-iterator")

# Records loaded and processed per Workflow run (one page).
PAGE_SIZE = 5


@dataclass
class SingleRecord:
    """One record in the dataset being processed."""

    id: int

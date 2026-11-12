"""Implements the iterator batch-processing pattern.

A single IteratorBatchWorkflow run processes one page of records in parallel,
starting one RecordProcessorWorkflow child per record. After every child
completes, the Workflow continues as new with the next offset, so each run's
Event History stays small no matter how large the dataset is.
"""

import asyncio
from datetime import timedelta

from temporalio import workflow

with workflow.unsafe.imports_passed_through():
    from activities import get_records
    from shared import SingleRecord


@workflow.defn
class RecordProcessorWorkflow:
    """Child Workflow that processes a single record."""

    @workflow.run
    async def run(self, record: SingleRecord) -> None:
        # Simulate 1-3 seconds of work. Kept short so the whole batch finishes in
        # seconds. workflow.random() is deterministic on replay, unlike random.
        await workflow.sleep(timedelta(seconds=workflow.random().randint(1, 3)))
        workflow.logger.info("Processed %s", record)


@workflow.defn
class IteratorBatchWorkflow:
    """Parent Workflow: loads a page, fans out children, continues as new."""

    @workflow.run
    async def run(self, page_size: int, offset: int) -> int:
        """Processes the batch and returns the total number of processed records.

        page_size is the number of records to process in a single Workflow run.
        offset is the offset of the first record to process; 0 starts the batch.
        """
        records = await workflow.execute_activity(
            get_records,
            args=[page_size, offset],
            start_to_close_timeout=timedelta(seconds=5),
        )

        # No more records in the dataset: the batch is done.
        if not records:
            return offset

        # Start one child per record with a human-friendly ID, e.g.
        # "batch-iterator-123/7", then wait for every child in this page. Error
        # handling is skipped for brevity.
        await asyncio.gather(
            *(
                workflow.execute_child_workflow(
                    RecordProcessorWorkflow.run,
                    record,
                    id=f"{workflow.info().workflow_id}/{record.id}",
                )
                for record in records
            )
        )

        workflow.continue_as_new(args=[page_size, offset + len(records)])

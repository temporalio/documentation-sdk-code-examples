import uuid

from temporalio.client import WorkflowExecutionStatus
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from activities import PAGE_COUNT, get_records
from shared import PAGE_SIZE
from workflows import IteratorBatchWorkflow, RecordProcessorWorkflow


async def test_batch_processes_every_record_across_continue_as_new():
    task_queue = str(uuid.uuid4())
    workflow_id = f"batch-iterator-{uuid.uuid4()}"
    # Time skipping makes the children's 1-3 second sleeps instant.
    async with await WorkflowEnvironment.start_time_skipping() as env:
        async with Worker(
            env.client,
            task_queue=task_queue,
            workflows=[IteratorBatchWorkflow, RecordProcessorWorkflow],
            activities=[get_records],
        ):
            handle = await env.client.start_workflow(
                IteratorBatchWorkflow.run,
                args=[PAGE_SIZE, 0],
                id=workflow_id,
                task_queue=task_queue,
            )
            assert await handle.result() == PAGE_SIZE * PAGE_COUNT

            # The batch continued as new, so the latest run differs from the first.
            latest = await env.client.get_workflow_handle(workflow_id).describe()
            assert latest.run_id != handle.first_execution_run_id

            # Every record got its own completed child Workflow.
            for record_id in range(PAGE_SIZE * PAGE_COUNT):
                child = await env.client.get_workflow_handle(
                    f"{workflow_id}/{record_id}"
                ).describe()
                assert child.status == WorkflowExecutionStatus.COMPLETED

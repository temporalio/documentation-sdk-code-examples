import asyncio
from contextlib import suppress
from datetime import timedelta
from uuid import uuid4

import pytest
from temporalio.common import PinnedVersioningOverride
from temporalio.service import RPCError, RPCStatusCode
from temporalio.testing import WorkflowEnvironment

modal_integration = pytest.importorskip("temporalio.worker_integrations.modal.function")

from modal_worker.worker import VERSION, configure
from modal_worker.workflows import GreetingWorkflow


@pytest.mark.asyncio
async def test_modal_worker_processes_workflow():
    async with await WorkflowEnvironment.start_local() as env:

        def configure_local(config):
            configure(config)
            config.client_connect_config.clear()
            config.client_connect_config.update(
                target_host=env.client.service_client.config.target_host,
                namespace=env.client.namespace,
                tls=False,
                identity="modal-worker-test",
            )

        worker = asyncio.create_task(
            modal_integration.run_worker(
                VERSION, configure_local, modal_function_timeout=timedelta(seconds=60)
            )
        )
        workflow_id = f"modal-worker-test-{uuid4()}"
        try:
            async with asyncio.timeout(30):
                while True:
                    if worker.done():
                        await worker
                        pytest.fail("Worker exited before the Workflow started")
                    try:
                        handle = await env.client.start_workflow(
                            GreetingWorkflow.run,
                            "World",
                            id=workflow_id,
                            task_queue="my-task-queue",
                            versioning_override=PinnedVersioningOverride(VERSION),
                        )
                        break
                    except RPCError as error:
                        if (
                            error.status != RPCStatusCode.FAILED_PRECONDITION
                            or "not present in task queue" not in str(error)
                        ):
                            raise
                        await asyncio.sleep(0.1)
                assert await handle.result() == "Hello, World!"
        finally:
            worker.cancel()
            with suppress(asyncio.CancelledError):
                await worker

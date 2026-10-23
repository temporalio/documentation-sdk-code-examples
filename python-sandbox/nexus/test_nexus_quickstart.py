import asyncio

import pytest
from google.protobuf.duration_pb2 import Duration
from temporalio.api.nexus.v1 import EndpointSpec, EndpointTarget
from temporalio.api.operatorservice.v1 import CreateNexusEndpointRequest
from temporalio.api.workflowservice.v1 import RegisterNamespaceRequest
from temporalio.client import Client
from temporalio.testing import WorkflowEnvironment

import caller_starter
import nexus_worker
from caller import NEXUS_ENDPOINT


@pytest.mark.asyncio
async def test_caller_workflow_calls_nexus_operation(monkeypatch, capsys):
    async with await WorkflowEnvironment.start_local() as env:
        # Step 5: create the caller Namespace and the Nexus Endpoint.
        await env.client.workflow_service.register_namespace(
            RegisterNamespaceRequest(
                namespace=caller_starter.NAMESPACE,
                workflow_execution_retention_period=Duration(seconds=86400),
            )
        )
        await env.client.operator_service.create_nexus_endpoint(
            CreateNexusEndpointRequest(
                spec=EndpointSpec(
                    name=NEXUS_ENDPOINT,
                    target=EndpointTarget(
                        worker=EndpointTarget.Worker(
                            namespace="default", task_queue="my-task-queue"
                        )
                    ),
                )
            )
        )

        real_connect = Client.connect
        target = env.client.service_client.config.target_host

        async def connect(_target, **kwargs):
            return await real_connect(target, **kwargs)

        monkeypatch.setattr(Client, "connect", connect)

        # Step 6: run the handler Worker, then the caller.
        handler_worker = asyncio.create_task(nexus_worker.main())
        try:
            await asyncio.wait_for(caller_starter.main(), timeout=60)
        finally:
            handler_worker.cancel()
            with pytest.raises(asyncio.CancelledError):
                await handler_worker
        assert "Workflow result: Hello Temporal" in capsys.readouterr().out

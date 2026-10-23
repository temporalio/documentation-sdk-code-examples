import asyncio

import pytest
from google.protobuf.duration_pb2 import Duration
from temporalio.api.nexus.v1 import EndpointSpec, EndpointTarget
from temporalio.api.operatorservice.v1 import CreateNexusEndpointRequest
from temporalio.api.workflowservice.v1 import RegisterNamespaceRequest
from temporalio.testing import WorkflowEnvironment

from nexus_standalone_operations import starter, worker

CALLER_NAMESPACE = "my-caller-namespace"
HANDLER_NAMESPACE = "my-handler-namespace"


@pytest.mark.asyncio
async def test_standalone_operations(monkeypatch, capsys):
    async with await WorkflowEnvironment.start_local(
        dev_server_extra_args=["--dynamic-config-value", "nexusoperation.enableStandalone=true"]
    ) as env:
        for namespace in [CALLER_NAMESPACE, HANDLER_NAMESPACE]:
            await env.client.workflow_service.register_namespace(
                RegisterNamespaceRequest(
                    namespace=namespace,
                    workflow_execution_retention_period=Duration(seconds=86400),
                )
            )
        await env.client.operator_service.create_nexus_endpoint(
            CreateNexusEndpointRequest(
                spec=EndpointSpec(
                    name=starter.ENDPOINT_NAME,
                    target=EndpointTarget(
                        worker=EndpointTarget.Worker(
                            namespace=HANDLER_NAMESPACE, task_queue=worker.TASK_QUEUE
                        )
                    ),
                )
            )
        )
        monkeypatch.setenv("TEMPORAL_ADDRESS", env.client.service_client.config.target_host)

        monkeypatch.setenv("TEMPORAL_NAMESPACE", HANDLER_NAMESPACE)
        worker_task = asyncio.create_task(worker.main())
        await asyncio.sleep(1)

        monkeypatch.setenv("TEMPORAL_NAMESPACE", CALLER_NAMESPACE)
        try:
            # Visibility is eventually consistent, so retry until both Operations are listed.
            for _ in range(20):
                await asyncio.wait_for(starter.main(), timeout=30)
                out = capsys.readouterr().out
                if "Total Nexus operations: 0" not in out:
                    break
                await asyncio.sleep(0.5)
        finally:
            worker.interrupt_event.set()
            await asyncio.wait_for(worker_task, timeout=30)

        assert "Echo result: hello" in out
        assert "HelloOutput(greeting='Hello, World!')" in out
        assert "Operation: echo" in out
        assert "Total Nexus operations:" in out

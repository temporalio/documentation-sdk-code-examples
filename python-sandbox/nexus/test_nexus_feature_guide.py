import asyncio
import uuid

import pytest
from google.protobuf.duration_pb2 import Duration
from temporalio import workflow
from temporalio.api.nexus.v1 import EndpointSpec, EndpointTarget
from temporalio.api.operatorservice.v1 import CreateNexusEndpointRequest
from temporalio.api.workflowservice.v1 import RegisterNamespaceRequest
from temporalio.client import Client
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from nexus_feature_guide import caller_app, worker
from nexus_feature_guide.caller_workflows import NEXUS_ENDPOINT
from nexus_feature_guide.messaging import (
    ApproveCallerWorkflow,
    ApproveInput,
    GreetingWorkflow,
    NexusGreetingServiceHandler,
)
from nexus_feature_guide.service import HelloInput, HelloOutput, MyNexusService


@workflow.defn
class HelloWithArgsCallerWorkflow:
    @workflow.run
    async def run(self, input: HelloInput) -> HelloOutput:
        nexus_client = workflow.create_nexus_client(
            service=MyNexusService, endpoint=NEXUS_ENDPOINT
        )
        return await nexus_client.execute_operation(MyNexusService.hello_with_args, input)


async def create_namespace(env, namespace):
    await env.client.workflow_service.register_namespace(
        RegisterNamespaceRequest(
            namespace=namespace, workflow_execution_retention_period=Duration(seconds=86400)
        )
    )


async def create_endpoint(env, name, namespace, task_queue):
    await env.client.operator_service.create_nexus_endpoint(
        CreateNexusEndpointRequest(
            spec=EndpointSpec(
                name=name,
                target=EndpointTarget(
                    worker=EndpointTarget.Worker(namespace=namespace, task_queue=task_queue)
                ),
            )
        )
    )


@pytest.mark.asyncio
async def test_caller_calls_sync_and_async_operations(monkeypatch, capsys):
    async with await WorkflowEnvironment.start_local() as env:
        await create_namespace(env, worker.NAMESPACE)
        await create_namespace(env, caller_app.NAMESPACE)
        await create_endpoint(env, NEXUS_ENDPOINT, worker.NAMESPACE, worker.TASK_QUEUE)

        real_connect = Client.connect
        target = env.client.service_client.config.target_host

        async def connect(_target, **kwargs):
            return await real_connect(target, **kwargs)

        monkeypatch.setattr(Client, "connect", connect)

        handler_worker = asyncio.create_task(worker.main())
        try:
            await asyncio.wait_for(caller_app.main(), timeout=60)
            out = capsys.readouterr().out
            assert "Sync Operation result: Hello Nexus" in out
            assert "Workflow Operation result: Hello Nexus" in out

            # The multiple-arguments Operation starts a Workflow with separate args.
            caller_client = await Client.connect("localhost:7233", namespace=caller_app.NAMESPACE)
            async with Worker(
                caller_client,
                task_queue="args-caller",
                workflows=[HelloWithArgsCallerWorkflow],
            ):
                result = await caller_client.execute_workflow(
                    HelloWithArgsCallerWorkflow.run,
                    HelloInput(name="Ada", language="fr"),
                    id=str(uuid.uuid4()),
                    task_queue="args-caller",
                )
                assert result.message == "Bonjour Ada"
        finally:
            handler_worker.cancel()
            with pytest.raises(asyncio.CancelledError):
                await handler_worker


@pytest.mark.asyncio
async def test_operation_signals_running_workflow():
    async with await WorkflowEnvironment.start_local() as env:
        task_queue = str(uuid.uuid4())
        await create_endpoint(env, "my-greeting-endpoint", env.client.namespace, task_queue)
        async with Worker(
            env.client,
            task_queue=task_queue,
            workflows=[GreetingWorkflow, ApproveCallerWorkflow],
            nexus_service_handlers=[NexusGreetingServiceHandler()],
        ):
            greeting = await env.client.start_workflow(
                GreetingWorkflow.run, id="GreetingWorkflow_for_user-1", task_queue=task_queue
            )
            await env.client.execute_workflow(
                ApproveCallerWorkflow.run,
                ApproveInput(user_id="user-1", name="Ada"),
                id=str(uuid.uuid4()),
                task_queue=task_queue,
            )
            assert await greeting.result() == "Approved by Ada"

# @@@SNIPSTART python-nexus-feature-guide-service-handler
import nexusrpc
from temporalio import nexus

from nexus_feature_guide.handler_workflows import (
    HelloArgsHandlerWorkflow,
    HelloHandlerWorkflow,
)
from nexus_feature_guide.service import (
    EchoInput,
    EchoOutput,
    HelloInput,
    HelloOutput,
    MyNexusService,
)


@nexusrpc.handler.service_handler(service=MyNexusService)
class MyNexusServiceHandler:
    @nexus.temporal_operation
    async def echo(
        self,
        _ctx: nexus.TemporalStartOperationContext,
        client: nexus.TemporalNexusClient,
        input: EchoInput,
    ) -> nexus.TemporalOperationResult[EchoOutput]:
        return nexus.TemporalOperationResult.sync(EchoOutput(message=input.message))

    @nexus.temporal_operation
    async def hello(
        self,
        _ctx: nexus.TemporalStartOperationContext,
        client: nexus.TemporalNexusClient,
        input: HelloInput,
    ) -> nexus.TemporalOperationResult[HelloOutput]:
        return await client.start_workflow(
            HelloHandlerWorkflow.run,
            input,
            id=f"hello-{input.name}-{input.language}",
        )

    @nexus.temporal_operation
    async def hello_with_args(
        self,
        _ctx: nexus.TemporalStartOperationContext,
        client: nexus.TemporalNexusClient,
        input: HelloInput,
    ) -> nexus.TemporalOperationResult[HelloOutput]:
        return await client.start_workflow(
            HelloArgsHandlerWorkflow.run,
            args=[input.name, input.language],
            id=f"hello-{input.name}-{input.language}",
        )
# @@@SNIPEND

# @@@SNIPSTART python-nexus-quickstart-handler
import uuid

import nexusrpc.handler
from temporalio import nexus

from service import MyInput, SayHelloNexusService
from workflows import SayHelloWorkflow


@nexusrpc.handler.service_handler(service=SayHelloNexusService)
class SayHelloNexusServiceHandler:
    @nexus.workflow_run_operation
    async def say_hello(
        self, ctx: nexus.WorkflowRunOperationContext, input: MyInput
    ) -> nexus.WorkflowHandle[str]:
        return await ctx.start_workflow(
            SayHelloWorkflow.run,
            input.name,
            id=f"say-hello-nexus-{uuid.uuid4()}",

            # Task queue defaults to the task queue this Operation is handled on.
        )
# @@@SNIPEND

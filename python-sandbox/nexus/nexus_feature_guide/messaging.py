"""A Nexus Service that Signals a Workflow that is already running."""

from dataclasses import dataclass

import nexusrpc
from temporalio import nexus, workflow
from temporalio.client import Client, WorkflowHandle


@dataclass
class ApproveInput:
    user_id: str
    name: str


@dataclass
class ApproveOutput:
    pass


@nexusrpc.service
class NexusGreetingService:
    approve: nexusrpc.Operation[ApproveInput, ApproveOutput]


@workflow.defn
class GreetingWorkflow:
    def __init__(self) -> None:
        self.approver: str | None = None

    @workflow.run
    async def run(self) -> str:
        await workflow.wait_condition(lambda: self.approver is not None)
        return f"Approved by {self.approver}"

    @workflow.signal
    def approve(self, input: ApproveInput) -> None:
        self.approver = input.name


# @@@SNIPSTART python-nexus-feature-guide-messaging
@nexusrpc.handler.service_handler(service=NexusGreetingService)
class NexusGreetingServiceHandler:
    def _get_workflow_handle(
        self, client: Client, user_id: str
    ) -> WorkflowHandle[GreetingWorkflow, str]:
        return client.get_workflow_handle_for(
            GreetingWorkflow.run, f"GreetingWorkflow_for_{user_id}"
        )

    @nexus.temporal_operation
    async def approve(
        self,
        _ctx: nexus.TemporalStartOperationContext,
        client: nexus.TemporalNexusClient,
        input: ApproveInput,
    ) -> nexus.TemporalOperationResult[ApproveOutput]:
        await self._get_workflow_handle(client.client, input.user_id).signal(
            GreetingWorkflow.approve, input
        )
        return nexus.TemporalOperationResult.sync(ApproveOutput())
# @@@SNIPEND


@workflow.defn
class ApproveCallerWorkflow:
    @workflow.run
    async def run(self, input: ApproveInput) -> None:
        nexus_client = workflow.create_nexus_client(
            service=NexusGreetingService, endpoint="my-greeting-endpoint"
        )
        await nexus_client.execute_operation(NexusGreetingService.approve, input)

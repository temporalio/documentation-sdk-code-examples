# @@@SNIPSTART python-nexus-feature-guide-caller-workflow
from temporalio import workflow

with workflow.unsafe.imports_passed_through():
    from nexus_feature_guide.service import (
        EchoInput,
        EchoOutput,
        HelloInput,
        HelloOutput,
        MyNexusService,
    )

NEXUS_ENDPOINT = "my-nexus-endpoint-name"


@workflow.defn
class CallerWorkflow:
    @workflow.run
    async def run(self, name: str) -> tuple[EchoOutput, HelloOutput]:
        nexus_client = workflow.create_nexus_client(
            service=MyNexusService,
            endpoint=NEXUS_ENDPOINT,
        )
        # Start the nexus operation and wait for the result in one go, using execute_operation.
        wf_result = await nexus_client.execute_operation(
            MyNexusService.hello,
            HelloInput(name=name, language="en"),
        )
        # Alternatively, you can use start_operation to obtain the operation handle and
        # then `await` the handle to obtain the result.
        sync_operation_handle = await nexus_client.start_operation(
            MyNexusService.echo,
            EchoInput(message=f"Hello {name}"),
        )
        sync_result = await sync_operation_handle
        return sync_result, wf_result
# @@@SNIPEND

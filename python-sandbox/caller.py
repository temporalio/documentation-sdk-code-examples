# @@@SNIPSTART python-nexus-quickstart-caller
from datetime import timedelta
from temporalio import workflow
from service import MyInput, SayHelloNexusService

NEXUS_ENDPOINT = "my-nexus-endpoint-name"


@workflow.defn
class CallerWorkflow:
    @workflow.run
    async def run(self, name: str) -> str:
        nexus_client = workflow.create_nexus_client(
            service=SayHelloNexusService,
            endpoint=NEXUS_ENDPOINT,
        )
        return await nexus_client.execute_operation(
            SayHelloNexusService.say_hello,
            MyInput(name=name),
            schedule_to_close_timeout=timedelta(seconds=10),
        )
# @@@SNIPEND

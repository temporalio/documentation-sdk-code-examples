import datetime

import temporalio.workflow
from temporalio import workflow


@workflow.defn
class UnrestrictedBlockWorkflow:
    @workflow.run
    async def run(self) -> str:
        # @@@SNIPSTART python-sdk-sandbox-unrestricted-block
        with temporalio.workflow.unsafe.sandbox_unrestricted():
            # Your code
            today = datetime.date.today()
        # @@@SNIPEND
        return today.isoformat()

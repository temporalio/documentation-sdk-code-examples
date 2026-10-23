# @@@SNIPSTART python-observability-workflow-span
from datetime import timedelta

from opentelemetry import trace
from temporalio import workflow

from observability.my_activities import my_activity


@workflow.defn
class MyWorkflow:
    @workflow.run
    async def run(self, name: str) -> str:
        with trace.get_tracer(__name__).start_as_current_span("prepare"):
            return await workflow.execute_activity(
                my_activity, name, start_to_close_timeout=timedelta(seconds=10)
            )
# @@@SNIPEND

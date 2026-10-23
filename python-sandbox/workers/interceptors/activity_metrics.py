# @@@SNIPSTART python-interceptors-activity-metrics
from datetime import datetime, timezone
from temporalio import activity
from temporalio.client import Client
from temporalio.worker import (
    ActivityInboundInterceptor,
    ExecuteActivityInput,
    Interceptor,
    Worker,
)

from interceptors.workflows import GreetingWorkflow, compose_greeting

class SimpleWorkerInterceptor(Interceptor):
    def intercept_activity(
        self, next: ActivityInboundInterceptor
    ) -> ActivityInboundInterceptor:
        return ActivityMetricsInterceptor(next)


class ActivityMetricsInterceptor(ActivityInboundInterceptor):
    async def execute_activity(self, input: ExecuteActivityInput):
        info = activity.info()
        meter = activity.metric_meter()
        attrs = {"workflow_type": info.workflow_type}

        # Before the activity executes: record Schedule-To-Start
        schedule_to_start = info.started_time - info.current_attempt_scheduled_time
        meter.create_histogram_timedelta(
            "custom_activity_schedule_to_start_latency",
            description="Time between activity scheduling and start",
            unit="duration",
        ).record(schedule_to_start, attrs)

        # Execute the activity
        result = await self.next.execute_activity(input)

        # After the activity completes: record Schedule-To-Close
        elapsed = datetime.now(timezone.utc) - info.current_attempt_scheduled_time
        meter.create_histogram_timedelta(
            "custom_activity_schedule_to_close_latency",
            description="Time between activity scheduling and completion",
            unit="duration",
        ).record(elapsed, attrs)

        return result

async def main():
    client = await Client.connect(
        "localhost:7233",
    )
    worker = Worker(
        client,
        task_queue="my-task-queue",
        workflows=[GreetingWorkflow],
        activities=[compose_greeting],
        interceptors=[SimpleWorkerInterceptor()],
    )
    await worker.run()
# @@@SNIPEND

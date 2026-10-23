from datetime import timedelta

from temporalio import workflow


@workflow.defn
class HeartbeatTimeoutWorkflow:
    @workflow.run
    async def run(self, name: str) -> list[str]:
        # @@@SNIPSTART python-activity-timeouts-start-activity-heartbeat-timeout
        handle = workflow.start_activity(
            activity="your-activity",
            schedule_to_close_timeout=timedelta(seconds=5),
            heartbeat_timeout=timedelta(seconds=1),
        )
        # @@@SNIPEND
        started_result = await handle

        # @@@SNIPSTART python-activity-timeouts-execute-activity-heartbeat-timeout
        executed_result = await workflow.execute_activity(
            activity="your-activity",
            arg=name,
            schedule_to_close_timeout=timedelta(seconds=5),
            heartbeat_timeout=timedelta(seconds=1),
        )
        # @@@SNIPEND
        return [started_result, executed_result]

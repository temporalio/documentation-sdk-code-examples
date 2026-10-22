from temporalio import workflow


def emit_metric(name: str, value: int) -> None:
    workflow.metric_meter().create_counter(name).add(value)


# @@@SNIPSTART python-workflow-basics-logger
@workflow.defn
class DeterministicApisWorkflow:
    @workflow.run
    async def run(self, name: str) -> str:
        workflow.logger.info("Starting workflow for %s", name)
        # @@@SNIPEND

        # @@@SNIPSTART python-workflow-basics-random
        value = workflow.random().randint(1, 100)
        unique_id = workflow.uuid4()
        # @@@SNIPEND

        # @@@SNIPSTART python-workflow-basics-now
        current_time = workflow.now()
        # @@@SNIPEND

        # @@@SNIPSTART python-workflow-basics-is-replaying
        if not workflow.unsafe.is_replaying():
            emit_metric("workflow_started", 1)
        # @@@SNIPEND

        return f"{name} {value} {unique_id} {current_time.isoformat()}"

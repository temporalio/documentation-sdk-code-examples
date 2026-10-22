from dataclasses import dataclass
from datetime import timedelta

from temporalio import activity, workflow


@activity.defn
async def check_title_validity(name_with_title: str) -> bool:
    # Pretend that we are calling a remote service that validates titles.
    return name_with_title.startswith("Sir ")


# @@@SNIPSTART python-message-passing-workflow-init
@dataclass
class MyWorkflowInput:
    name: str


@workflow.defn
class WorkflowRunSeesWorkflowInitWorkflow:
    @workflow.init
    def __init__(self, workflow_input: MyWorkflowInput) -> None:
        self.name_with_title = f"Sir {workflow_input.name}"
        self.title_has_been_checked = False

    @workflow.run
    async def get_greeting(self, workflow_input: MyWorkflowInput) -> str:
        await workflow.wait_condition(lambda: self.title_has_been_checked)
        return f"Hello, {self.name_with_title}"

    @workflow.update
    async def check_title_validity(self) -> bool:
        # 👉 The handler is now guaranteed to see the workflow input
        # after it has been processed by __init__.
        is_valid = await workflow.execute_activity(
            check_title_validity,
            self.name_with_title,
            schedule_to_close_timeout=timedelta(seconds=10),
        )
        self.title_has_been_checked = True
        return is_valid
# @@@SNIPEND

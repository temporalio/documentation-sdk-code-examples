from dataclasses import dataclass

from temporalio import workflow

# @@@SNIPSTART python-workflow-basics-init
@dataclass
class MyWorkflowInput:
    name: str


@workflow.defn
class WorkflowRunSeesWorkflowInitWorkflow:
    @workflow.init
    def __init__(self, workflow_input: MyWorkflowInput) -> None:
        self.name_with_title = f"Knight {workflow_input.name}"
        self.title_has_been_checked = False

    @workflow.run
    async def get_greeting(self, workflow_input: MyWorkflowInput) -> str:
        await workflow.wait_condition(lambda: self.title_has_been_checked)
        return f"Hello, {self.name_with_title}"
    # @@@SNIPEND

    @workflow.update
    async def check_title(self, title: str) -> bool:
        self.title_has_been_checked = True
        return self.name_with_title.startswith(title)

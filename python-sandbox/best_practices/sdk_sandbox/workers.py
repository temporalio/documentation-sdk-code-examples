import datetime

from temporalio import workflow
from temporalio.client import Client

from sdk_sandbox.passthrough_workflow import MyWorkflow


@workflow.defn
class TodayWorkflow:
    """Calls datetime.date.today(), which the default sandbox restricts."""

    @workflow.run
    async def run(self) -> str:
        return datetime.date.today().isoformat()


def passthrough_worker(client: Client, task_queue: str):
    # @@@SNIPSTART python-sdk-sandbox-passthrough-worker
    from temporalio.worker import Worker
    from temporalio.worker.workflow_sandbox import SandboxedWorkflowRunner, SandboxRestrictions

    my_worker = Worker(
      client,
      task_queue=task_queue,
      workflows=[MyWorkflow],
      workflow_runner=SandboxedWorkflowRunner(
        restrictions=SandboxRestrictions.default.with_passthrough_modules("pydantic")
      )
    )
    # @@@SNIPEND
    return my_worker


def unrestricted_today_worker(client: Client, task_queue: str):
    # @@@SNIPSTART python-sdk-sandbox-unrestrict-member
    import dataclasses
    from temporalio.worker import Worker
    from temporalio.worker.workflow_sandbox import SandboxedWorkflowRunner, SandboxRestrictions

    my_restrictions = dataclasses.replace(
        SandboxRestrictions.default,
        invalid_module_members=SandboxRestrictions.invalid_module_members_default.with_child_unrestricted(
          "datetime", "date", "today",
        ),
    )
    my_worker = Worker(
        client,
        task_queue=task_queue,
        workflows=[TodayWorkflow],
        workflow_runner=SandboxedWorkflowRunner(restrictions=my_restrictions),
    )
    # @@@SNIPEND
    return my_worker


def restricted_date_worker(client: Client, task_queue: str):
    # @@@SNIPSTART python-sdk-sandbox-restrict-member
    import dataclasses
    from temporalio.worker import Worker
    from temporalio.worker.workflow_sandbox import (
      SandboxedWorkflowRunner,
      SandboxMatcher,
      SandboxRestrictions,
    )

    my_restrictions = dataclasses.replace(
        SandboxRestrictions.default,
        invalid_module_members=SandboxRestrictions.invalid_module_members_default | SandboxMatcher(
          children={"datetime": SandboxMatcher(use={"date"})},
        ),
    )
    my_worker = Worker(
        client,
        task_queue=task_queue,
        workflows=[TodayWorkflow],
        workflow_runner=SandboxedWorkflowRunner(restrictions=my_restrictions),
    )
    # @@@SNIPEND
    return my_worker


def notification_policy_worker(client: Client, task_queue: str):
    # @@@SNIPSTART python-sdk-sandbox-notification-worker
    from temporalio import workflow
    from temporalio.worker import Worker
    from temporalio.worker.workflow_sandbox import SandboxedWorkflowRunner, SandboxRestrictions

    my_worker = Worker(
      client,
      task_queue=task_queue,
      workflows=[MyWorkflow],
      workflow_runner=SandboxedWorkflowRunner(
        restrictions=SandboxRestrictions.default.with_import_notification_policy(
          workflow.SandboxImportNotificationPolicy.WARN_ON_DYNAMIC_IMPORT | workflow.SandboxImportNotificationPolicy.WARN_ON_UNINTENTIONAL_PASSTHROUGH
        )
      )
    )
    # @@@SNIPEND
    return my_worker

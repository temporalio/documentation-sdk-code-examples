import pytest
from temporalio.client import Client, ScheduleActionStartWorkflow
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from schedules import (
    backfill_schedule,
    create_schedule,
    cron_workflow,
    delete_schedule,
    describe_schedule,
    list_schedules,
    pause_schedule,
    start_delay,
    trigger_schedule,
    update_schedule,
)
from schedules.your_workflows import CronWorkflow, YourSchedulesWorkflow, YourWorkflow


def patch_connect(monkeypatch, env):
    async def connect(*args, **kwargs):
        return env.client

    monkeypatch.setattr(Client, "connect", connect)


@pytest.mark.asyncio
async def test_schedule_lifecycle(monkeypatch, capsys):
    async with await WorkflowEnvironment.start_local() as env:
        patch_connect(monkeypatch, env)
        async with Worker(
            env.client,
            task_queue="schedules-task-queue",
            workflows=[YourSchedulesWorkflow],
        ):
            await create_schedule.main()
            handle = env.client.get_schedule_handle("workflow-schedule-id")

            await describe_schedule.main()
            assert "Returns the note: Here's a note on my Schedule." in capsys.readouterr().out

            await list_schedules.main()
            await trigger_schedule.main()
            await backfill_schedule.main()
            assert "Backfilled Schedule: workflow-schedule-id" in capsys.readouterr().out

            await pause_schedule.main()
            desc = await handle.describe()
            assert desc.schedule.state.paused
            assert desc.schedule.state.note == "Pausing the schedule for now"

            await update_schedule.main()
            action = (await handle.describe()).schedule.action
            assert isinstance(action, ScheduleActionStartWorkflow)
            assert [
                env.client.data_converter.payload_converter.from_payload(a, str)
                for a in action.args
            ] == ["my new schedule arg"]

            await delete_schedule.main()
            schedule_ids = [s.id async for s in await env.client.list_schedules()]
            assert "workflow-schedule-id" not in schedule_ids


@pytest.mark.asyncio
async def test_cron_workflow(monkeypatch, capsys):
    async with await WorkflowEnvironment.start_local() as env:
        patch_connect(monkeypatch, env)
        await cron_workflow.main()
        assert "Started Cron Workflow: your-workflow-id" in capsys.readouterr().out
        desc = await env.client.get_workflow_handle("your-workflow-id").describe()
        assert desc.raw_description.workflow_execution_info.HasField("execution_time")
        await env.client.get_workflow_handle("your-workflow-id").terminate()


@pytest.mark.asyncio
async def test_start_delay(monkeypatch, capsys):
    async with await WorkflowEnvironment.start_time_skipping() as env:
        patch_connect(monkeypatch, env)
        async with Worker(
            env.client, task_queue="your-task-queue", workflows=[YourWorkflow]
        ):
            await start_delay.main()
        assert "Result: Hello, your name!" in capsys.readouterr().out

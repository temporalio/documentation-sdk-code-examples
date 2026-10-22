# @@@SNIPSTART python-workflow-streams-order-workflow
from dataclasses import dataclass
from datetime import timedelta

from temporalio import workflow
from temporalio.contrib.workflow_streams import WorkflowStream

with workflow.unsafe.imports_passed_through():
    from workflow_streams.order_activities import (
        charge_payment_activity,
        dispatch_order_activity,
        validate_order_activity,
    )


@dataclass
class OrderInput:
    order_id: str


@dataclass
class StatusEvent:
    state: str
    progress: int = 0
    detail: str = ""


@workflow.defn
class OrderWorkflow:
    @workflow.init
    def __init__(self, input: OrderInput) -> None:
        self.stream = WorkflowStream()
        self.status = self.stream.topic("status", type=StatusEvent)

    @workflow.run
    async def run(self, input: OrderInput) -> str:
        self.status.publish(StatusEvent(state="validating", detail="checking inventory"))
        await validate_order(input.order_id)

        self.status.publish(StatusEvent(state="charging", progress=33, detail="authorizing payment"))
        await charge_payment(input.order_id)

        self.status.publish(StatusEvent(state="shipping", progress=66, detail="dispatching to warehouse"))
        result = await dispatch_order(input.order_id)

        self.status.publish(StatusEvent(state="completed", progress=100))
        await workflow.sleep(timedelta(seconds=30))
        return result
# @@@SNIPEND


@dataclass
class ProgressEvent:
    message: str


async def validate_order(order_id: str) -> None:
    await workflow.execute_activity(
        validate_order_activity, order_id, start_to_close_timeout=timedelta(seconds=10)
    )


async def charge_payment(order_id: str) -> None:
    await workflow.execute_activity(
        charge_payment_activity, order_id, start_to_close_timeout=timedelta(seconds=10)
    )


async def dispatch_order(order_id: str) -> str:
    return await workflow.execute_activity(
        dispatch_order_activity, order_id, start_to_close_timeout=timedelta(seconds=10)
    )

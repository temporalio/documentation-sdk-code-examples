import datetime
import ipaddress
from datetime import timedelta

from pydantic import BaseModel
from temporalio import activity, workflow


class Order(BaseModel):
    order_id: str
    placed_at: datetime.datetime
    items: list[str]


@activity.defn
async def confirm_order(order: Order) -> Order:
    return order.model_copy(update={"items": [*order.items, "receipt"]})


@workflow.defn
class OrderWorkflow:
    @workflow.run
    async def run(self, order: Order) -> Order:
        return await workflow.execute_activity(
            confirm_order, order, start_to_close_timeout=timedelta(seconds=10)
        )


@workflow.defn
class IPv4Workflow:
    @workflow.run
    async def run(self, addresses: list[ipaddress.IPv4Address]) -> ipaddress.IPv4Address:
        return max(addresses)


@workflow.defn
class SingleIPv4Workflow:
    @workflow.run
    async def run(self, address: ipaddress.IPv4Address) -> ipaddress.IPv4Address:
        return address + 1

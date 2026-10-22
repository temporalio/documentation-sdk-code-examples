from dataclasses import dataclass
from datetime import timedelta
from typing import List, Optional

from temporalio import activity, workflow
from temporalio.exceptions import ApplicationError


@dataclass
class ShoppingCartItem:
    sku: str
    quantity: int


@activity.defn
async def get_price(item: ShoppingCartItem) -> Optional[int]:
    # Pretend that we are calling a remote pricing service. Prices are in cents.
    prices = {"sku-123": 599, "sku-456": 1299}
    price = prices.get(item.sku)
    return None if price is None else price * item.quantity


@workflow.defn
class ShoppingCartWorkflow:
    def __init__(self) -> None:
        self.items: List[ShoppingCartItem] = []
        self.total_cents = 0
        self.checked_out = False

    @workflow.run
    async def run(self) -> List[ShoppingCartItem]:
        await workflow.wait_condition(lambda: self.checked_out)
        await workflow.wait_condition(workflow.all_handlers_finished)
        return self.items

    @workflow.update
    async def add_item(self, item: ShoppingCartItem) -> str:
        price = await workflow.execute_activity(
            get_price, item, start_to_close_timeout=timedelta(seconds=10)
        )
        if price is None:
            raise ApplicationError(f"Item unavailable: {item.sku}")
        self.items.append(item)
        self.total_cents += price
        return f"{self.total_cents / 100:.2f}"

    @workflow.signal
    def checkout(self) -> None:
        self.checked_out = True

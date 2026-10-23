from decimal import Decimal
from typing import Optional, Tuple

from temporalio import common
from temporalio.client import (
    Client,
    WithStartWorkflowOperation,
    WorkflowHandle,
    WorkflowUpdateFailedError,
)

from message_passing.shopping_cart import ShoppingCartItem, ShoppingCartWorkflow


async def handle_add_item_request(
    temporal_client: Client, cart_id: str, item_id: str, quantity: int
) -> Tuple[Optional[Decimal], WorkflowHandle]:
    # @@@SNIPSTART python-message-passing-update-with-start
    start_op = WithStartWorkflowOperation(
        ShoppingCartWorkflow.run,
        id=cart_id,
        id_conflict_policy=common.WorkflowIDConflictPolicy.USE_EXISTING,
        task_queue="my-task-queue",
    )
    try:
        price = Decimal(
            await temporal_client.execute_update_with_start_workflow(
                ShoppingCartWorkflow.add_item,
                ShoppingCartItem(sku=item_id, quantity=quantity),
                start_workflow_operation=start_op,
            )
        )
    except WorkflowUpdateFailedError:
        price = None

    workflow_handle = await start_op.workflow_handle()

    return price, workflow_handle
    # @@@SNIPEND

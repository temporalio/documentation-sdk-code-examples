from dataclasses import dataclass

from temporalio import activity
from temporalio.exceptions import ApplicationError


@dataclass
class Order:
    order_id: str
    fail_shipment: bool = False


# Records which steps and compensations ran, so tests can check the rollback order.
calls: list[str] = []


@activity.defn
async def reserve_inventory(order: Order) -> None:
    calls.append("reserve_inventory")


@activity.defn
async def revert_inventory(order: Order) -> None:
    calls.append("revert_inventory")


@activity.defn
async def charge_payment(order: Order) -> str:
    calls.append("charge_payment")
    return f"payment-{order.order_id}"


@activity.defn
async def refund_payment(order: Order) -> None:
    calls.append("refund_payment")


@activity.defn
async def create_shipment(order: Order) -> str:
    calls.append("create_shipment")
    if order.fail_shipment:
        raise ApplicationError("No couriers available", non_retryable=True)
    return f"shipment-{order.order_id}"


@activity.defn
async def cancel_shipment(payment_id: str) -> None:
    calls.append("cancel_shipment")


# @@@SNIPSTART python-error-handling-saga
from temporalio import workflow
from temporalio.exceptions import ActivityError, ApplicationError
from datetime import timedelta

@workflow.defn
class OrderWorkflow:
    @workflow.run
    async def run(self, order: Order):
        compensations = []

        try:
            # Reserve inventory
            compensations.append({
                "activity": revert_inventory,
                "input": order
            })
            await workflow.execute_activity(
                reserve_inventory,
                order,
                start_to_close_timeout=timedelta(seconds=10),
            )

            # Charge payment
            compensations.append({
                "activity": refund_payment,
                "input": order
            })
            payment_id = await workflow.execute_activity(
                charge_payment,
                order,
                start_to_close_timeout=timedelta(seconds=10),
            )

            # Create shipment
            compensations.append({
                "activity": cancel_shipment,
                "input": payment_id
            })
            shipment_id = await workflow.execute_activity(
                create_shipment,
                order,
                start_to_close_timeout=timedelta(seconds=10),
            )

            return {"payment_id": payment_id, "shipment_id": shipment_id}

        except ActivityError as e:
            workflow.logger.error(f"Order failed: {e.cause}, rolling back...")

            # Execute compensations in reverse order
            for compensation in reversed(compensations):
                try:
                    await workflow.execute_activity(
                        compensation["activity"],
                        compensation["input"],
                        start_to_close_timeout=timedelta(seconds=10),
                    )
                except ActivityError as comp_err:
                    # Log compensation failure but continue with others
                    workflow.logger.error(f"Compensation failed: {comp_err.cause}")

            # Re-raise the original error
            raise ApplicationError(
                f"Order failed: {e.cause}",
                type="OrderFailed"
            )
# @@@SNIPEND

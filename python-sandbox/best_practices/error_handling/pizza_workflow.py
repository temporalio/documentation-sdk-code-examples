from dataclasses import dataclass

from temporalio import activity


@dataclass
class Distance:
    kilometers: float


@dataclass
class PizzaOrder:
    address: str
    is_delivery: bool


@activity.defn
async def calculate_distance(address: str) -> Distance:
    # Pretend that we are calling a mapping service.
    return Distance(kilometers=40.0 if "far" in address else 5.0)


MAX_DELIVERY_DISTANCE = 25


# @@@SNIPSTART python-error-handling-fail-workflow
from datetime import timedelta

from temporalio import workflow
from temporalio.exceptions import ApplicationError

@workflow.defn
class PizzaDeliveryWorkflow:
    @workflow.run
    async def run(self, order: PizzaOrder):
        distance = await workflow.execute_activity(
            calculate_distance,
            order.address,
            start_to_close_timeout=timedelta(seconds=10)
        )

        if order.is_delivery and distance.kilometers > 25:
            workflow.logger.error("Customer outside service area")
            raise ApplicationError(
                "Customer lives outside the service area",
                type="CustomerOutsideServiceArea"
            )

        # Continue with order...
        # @@@SNIPEND
        return "Order accepted"


@workflow.defn
class DeliveryRadiusWorkflow:
    @workflow.run
    async def run(self, address: str) -> str:
        distance = await workflow.execute_activity(
            calculate_distance, address, start_to_close_timeout=timedelta(seconds=10)
        )
        # @@@SNIPSTART python-error-handling-permanent-failure
        if distance.kilometers > MAX_DELIVERY_DISTANCE:
            # Retrying won't change the distance - this is permanent
            raise ApplicationError(
                "Customer lives outside service area",
                type="OutsideServiceArea"
            )
        # @@@SNIPEND
        return "In range"


@workflow.defn
class BuggyWorkflow:
    @workflow.run
    async def run(self) -> None:
        # @@@SNIPSTART python-error-handling-workflow-task-failure
        # This causes a Workflow Task failure (retries automatically)
        raise ValueError("Unexpected condition")
        # @@@SNIPEND

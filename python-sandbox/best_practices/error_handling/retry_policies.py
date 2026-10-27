from dataclasses import dataclass

from temporalio import activity


@dataclass
class Order:
    order_id: str


@activity.defn
async def call_external_service(order: Order) -> str:
    return f"processed {order.order_id}"


@activity.defn
async def process_order(order: Order) -> str:
    return f"processed {order.order_id}"


# @@@SNIPSTART python-error-handling-custom-retry-policy
from temporalio import workflow
from temporalio.common import RetryPolicy
from datetime import timedelta

@workflow.defn
class OrderWorkflow:
    @workflow.run
    async def run(self, order: Order):
        # Custom retry for rate-limited service
        retry_policy = RetryPolicy(
            initial_interval=timedelta(seconds=10),
            backoff_coefficient=3.0,
            maximum_interval=timedelta(minutes=5),
            maximum_attempts=20,
        )

        result = await workflow.execute_activity(
            call_external_service,
            order,
            start_to_close_timeout=timedelta(seconds=30),
            retry_policy=retry_policy,
        )
        return result
# @@@SNIPEND


@workflow.defn
class TwoPoliciesWorkflow:
    @workflow.run
    async def run(self, order: Order) -> None:
        # @@@SNIPSTART python-error-handling-different-policies
        fast_retry = RetryPolicy(
            initial_interval=timedelta(seconds=1),
            backoff_coefficient=1.5,
        )

        slow_retry = RetryPolicy(
            initial_interval=timedelta(seconds=30),
            backoff_coefficient=3.0,
        )

        # Same Activity, different policies
        await workflow.execute_activity(
            process_order,
            order,
            start_to_close_timeout=timedelta(seconds=10),
            retry_policy=fast_retry,
        )

        # Later, with different circumstances...
        await workflow.execute_activity(
            process_order,
            order,
            start_to_close_timeout=timedelta(seconds=10),
            retry_policy=slow_retry,
        )
        # @@@SNIPEND

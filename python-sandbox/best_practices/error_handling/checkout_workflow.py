from dataclasses import dataclass

from temporalio import activity
from temporalio.exceptions import ApplicationError


@dataclass
class PaymentDetails:
    card_number: str
    amount: float


@activity.defn
async def process_payment(payment_details: PaymentDetails) -> str:
    if payment_details.amount > 1000:
        # Retryable as far as the Activity knows; the caller's Retry Policy decides.
        raise ApplicationError("Card has insufficient funds", type="InsufficientFunds")
    return f"charged {payment_details.amount}"


# @@@SNIPSTART python-error-handling-non-retryable-error-types
from temporalio import workflow
from temporalio.common import RetryPolicy
from temporalio.exceptions import ActivityError
from datetime import timedelta

@workflow.defn
class CheckoutWorkflow:
    @workflow.run
    async def run(self, payment_details: PaymentDetails):
        retry_policy = RetryPolicy(
            non_retryable_error_types=[
                "InvalidCardFormat",
                "InsufficientFunds",
                "AccountClosed",
            ]
        )

        try:
            result = await workflow.execute_activity(
                process_payment,
                payment_details,
                start_to_close_timeout=timedelta(seconds=30),
                retry_policy=retry_policy,
            )
            return result
        except ActivityError as e:
            workflow.logger.error(f"Payment failed: {e.cause}")
            # Handle the non-retryable error...
            # @@@SNIPEND
            return f"Payment declined: {e.cause}"

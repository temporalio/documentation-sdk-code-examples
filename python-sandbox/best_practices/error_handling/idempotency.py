# @@@SNIPSTART python-error-handling-idempotency-key
from temporalio import activity

@activity.defn
async def process_payment(amount: float, account: str):
    info = activity.info()
    idempotency_key = f"{info.workflow_run_id}-{info.activity_id}"

    # Pass idempotency_key to your payment service
    result = await payment_service.charge(
        amount=amount,
        account=account,
        idempotency_key=idempotency_key
    )
    return result
# @@@SNIPEND


class PaymentService:
    """Stand-in for a payment service that deduplicates requests by idempotency key."""

    def __init__(self) -> None:
        self.charges: dict[str, str] = {}

    async def charge(self, amount: float, account: str, idempotency_key: str) -> str:
        # A repeated key returns the original charge instead of charging again.
        return self.charges.setdefault(
            idempotency_key, f"charged {amount} to {account}"
        )


payment_service = PaymentService()

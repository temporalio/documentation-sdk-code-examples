from dataclasses import dataclass

from temporalio import activity


@dataclass
class TransferDetails:
    amount: int
    source_account: str
    target_account: str


@activity.defn
async def withdraw(details: TransferDetails) -> str:
    return f"withdrew {details.amount} from {details.source_account}"


@activity.defn
async def deposit(details: TransferDetails) -> str:
    if details.target_account == "closed-account":
        raise ValueError(f"Account {details.target_account} is closed")
    return f"deposited {details.amount} to {details.target_account}"


@activity.defn
async def refund(withdraw_result: str) -> str:
    return f"refunded: {withdraw_result}"


# @@@SNIPSTART python-error-handling-handle-activity-errors
from temporalio import workflow
from temporalio.common import RetryPolicy
from temporalio.exceptions import ActivityError, ApplicationError
from datetime import timedelta

@workflow.defn
class MoneyTransferWorkflow:
    @workflow.run
    async def run(self, details: TransferDetails):
        # Withdraw money
        try:
            withdraw_result = await workflow.execute_activity(
                withdraw,
                details,
                start_to_close_timeout=timedelta(seconds=10)
            )
        except ActivityError as e:
            raise ApplicationError(
                f"Withdrawal failed: {e.cause}",
                type="WithdrawalError"
            )

        # Deposit money
        try:
            deposit_result = await workflow.execute_activity(
                deposit,
                details,
                start_to_close_timeout=timedelta(seconds=10),
                # Limit retries so a deposit that keeps failing reaches the refund below
                retry_policy=RetryPolicy(
                    initial_interval=timedelta(seconds=1),
                    maximum_attempts=3,
                ),
            )
        except ActivityError as e:
            # Deposit failed - attempt refund
            try:
                await workflow.execute_activity(
                    refund,
                    withdraw_result,
                    start_to_close_timeout=timedelta(seconds=10)
                )
                raise ApplicationError(
                    f"Deposit failed but money refunded to source account",
                    type="DepositError"
                )
            except ActivityError as refund_err:
                raise ApplicationError(
                    f"Deposit failed and refund also failed: {refund_err.cause}",
                    type="CriticalTransferError"
                )

        return f"Transfer complete: {withdraw_result}, {deposit_result}"
# @@@SNIPEND

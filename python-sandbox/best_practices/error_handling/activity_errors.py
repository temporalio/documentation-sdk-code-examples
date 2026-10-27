# @@@SNIPSTART python-error-handling-raise-from-activity
from temporalio import activity
from temporalio.exceptions import ApplicationError

@activity.defn
async def validate_charge(credit_card_number: str, amount: float):
    if not is_valid_card(credit_card_number):
        raise ApplicationError(
            f"Invalid credit card number: {credit_card_number}",
            type="InvalidCreditCard",
        )

    if amount <= 0:
        raise ApplicationError(
            f"Amount must be positive, got {amount}",
            type="InvalidAmount",
        )

    return True
# @@@SNIPEND


def is_valid_card(credit_card_number: str) -> bool:
    return credit_card_number.isdigit() and len(credit_card_number) == 16

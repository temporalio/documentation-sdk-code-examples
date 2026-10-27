# @@@SNIPSTART python-error-handling-non-retryable
from temporalio import activity
from temporalio.exceptions import ApplicationError

@activity.defn
async def process_payment(card_number: str, amount: float):
    if not is_valid_card_format(card_number):
        # Invalid format will never become valid through retries
        raise ApplicationError(
            f"Invalid credit card format: {card_number}",
            type="InvalidCardFormat",
            non_retryable=True,
        )

    if amount <= 0:
        # Invalid amount won't be fixed by retrying
        raise ApplicationError(
            f"Amount must be positive: {amount}",
            type="InvalidAmount",
            non_retryable=True,
        )

    # Process payment...
    # @@@SNIPEND
    return f"charged {amount}"


def is_valid_card_format(card_number: str) -> bool:
    return card_number.isdigit() and len(card_number) == 16

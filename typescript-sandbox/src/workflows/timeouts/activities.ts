import { ApplicationFailure } from '@temporalio/activity';

// @@@SNIPSTART typescript-workflow-timeouts-custom-error
class InvalidChargeError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'InvalidChargeError';
    Object.setPrototypeOf(this, InvalidChargeError.prototype);
  }
}

export async function chargeWithCustomError(chargeAmount: number): Promise<void> {
  if (chargeAmount < 0) {
    throw new InvalidChargeError(`Invalid charge amount: ${chargeAmount} (must be above zero)`);
  }
}
// @@@SNIPEND

// @@@SNIPSTART typescript-workflow-timeouts-application-failure
export async function chargeWithApplicationFailure(chargeAmount: number): Promise<void> {
  if (chargeAmount < 0) {
    throw ApplicationFailure.create({
      message: `Invalid charge amount: ${chargeAmount} (must be above zero)`,
      type: 'InvalidChargeError',
    });
  }
}
// @@@SNIPEND

// @@@SNIPSTART typescript-workflow-timeouts-non-retryable
export async function chargeNonRetryable(chargeAmount: number): Promise<void> {
  if (chargeAmount < 0) {
    throw ApplicationFailure.create({
      message: `Invalid charge amount: ${chargeAmount} (must be above zero)`,
      nonRetryable: true,
    });
  }
}
// @@@SNIPEND

export async function addAddress(): Promise<void> {
  throw ApplicationFailure.create({ message: 'Address is not valid', nonRetryable: true });
}

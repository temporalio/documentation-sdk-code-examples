import { ActivityFailure, ApplicationFailure, log, proxyActivities } from '@temporalio/workflow';
import type * as activities from './activities';

const { addAddress, chargeWithCustomError, chargeWithApplicationFailure, chargeNonRetryable } = proxyActivities<
  typeof activities
>({
  startToCloseTimeout: '1 minute',
  retry: { maximumAttempts: 3, initialInterval: '10 milliseconds' },
});

export async function chargeWorkflow(
  method: 'customError' | 'applicationFailure' | 'nonRetryable',
  chargeAmount: number,
): Promise<void> {
  const charge = {
    customError: chargeWithCustomError,
    applicationFailure: chargeWithApplicationFailure,
    nonRetryable: chargeNonRetryable,
  }[method];
  await charge(chargeAmount);
}

export async function addAddressWorkflow(): Promise<void> {
  // @@@SNIPSTART typescript-workflow-timeouts-fail-workflow
  try {
    await addAddress();
  } catch (err) {
    if (err instanceof ActivityFailure && err.cause instanceof ApplicationFailure) {
      log.error(err.cause.message);
      throw err;
    }
  }
  // @@@SNIPEND
}

export async function example(): Promise<string> {
  return 'done';
}

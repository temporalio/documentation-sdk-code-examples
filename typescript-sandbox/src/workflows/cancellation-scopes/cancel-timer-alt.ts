// @@@SNIPSTART typescript-cancellation-scopes-cancel-timer-alt
import { CancelledFailure, CancellationScope, sleep } from '@temporalio/workflow';

export async function cancelTimerAltImpl(): Promise<void> {
  try {
    const scope = new CancellationScope();
    const promise = scope.run(() => sleep(1));
    scope.cancel(); // <-- Cancel the timer created in scope
    await promise; // <-- Throws CancelledFailure
  } catch (e) {
    if (e instanceof CancelledFailure) {
      console.log('Timer cancelled 👍');
    } else {
      throw e; // <-- Fail the workflow
    }
  }
}
// @@@SNIPEND

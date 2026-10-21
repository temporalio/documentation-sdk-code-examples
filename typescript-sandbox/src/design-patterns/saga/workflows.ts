import { proxyActivities } from '@temporalio/workflow';
import type * as activities from './activities';
import type { OpenAccountRequest } from './shared';

const acts = proxyActivities<typeof activities>({
  startToCloseTimeout: '10 seconds',
});

// @@@SNIPSTART typescript-pattern-saga-pattern-workflow
type Compensation = () => Promise<void>;

export async function openAccount(req: OpenAccountRequest): Promise<string> {
  const compensations: Compensation[] = [];

  try {
    // Step 1: createAccount has no compensation. Leaving an empty account
    // stub on later failure is acceptable.
    await acts.createAccount(req);

    // Register compensation for Step 2 BEFORE execution
    compensations.unshift(() => acts.clearPostalAddresses(req));
    await acts.addAddress(req);

    // Register compensation for Step 3 BEFORE execution
    compensations.unshift(() => acts.removeClient(req));
    await acts.addClient(req);

    // Register compensation for Step 4 BEFORE execution
    compensations.unshift(() => acts.disconnectBankAccounts(req));
    await acts.addBankAccount(req);

    return `Account ${req.accountId} opened`;
  } catch (err) {
    // On error, run all compensations in reverse order (unshift keeps them in LIFO already)
    for (const compensate of compensations) {
      await compensate();
    }
    // Re-throw so the Workflow Execution reports the original failure
    throw err;
  }
}
// @@@SNIPEND

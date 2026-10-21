import { ApplicationFailure, log } from '@temporalio/activity';
import type { OpenAccountRequest } from './shared';

// Forward steps. Each one would call a separate service in a real application.

export async function createAccount(req: OpenAccountRequest): Promise<void> {
  log.info(`Created account ${req.accountId}`);
}

export async function addAddress(req: OpenAccountRequest): Promise<void> {
  log.info(`Added address "${req.address}" to account ${req.accountId}`);
}

export async function addClient(req: OpenAccountRequest): Promise<void> {
  log.info(`Added client ${req.clientEmail} to account ${req.accountId}`);
}

export async function addBankAccount(req: OpenAccountRequest): Promise<void> {
  // Fail on purpose so the Workflow runs its compensations.
  // Non-retryable, so the failure reaches the Workflow without retry delays.
  throw ApplicationFailure.nonRetryable(
    `Bank rejected account ${req.bankAccountId}`,
    'BankAccountRejected',
  );
}

// Compensations. Each one is idempotent and safe to run when its forward step
// never ran, because the Workflow registers it before running that step.

export async function clearPostalAddresses(req: OpenAccountRequest): Promise<void> {
  log.info(`Compensation: cleared postal addresses for account ${req.accountId}`);
}

export async function removeClient(req: OpenAccountRequest): Promise<void> {
  log.info(`Compensation: removed client ${req.clientEmail} from account ${req.accountId}`);
}

export async function disconnectBankAccounts(req: OpenAccountRequest): Promise<void> {
  log.info(`Compensation: disconnected bank accounts from account ${req.accountId}`);
}

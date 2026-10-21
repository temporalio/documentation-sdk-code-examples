export const TASK_QUEUE = 'design-patterns-saga';

export interface OpenAccountRequest {
  accountId: string;
  address: string;
  clientEmail: string;
  bankAccountId: string;
}

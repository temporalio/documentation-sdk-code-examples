import * as wf from '@temporalio/workflow';

export interface TransactionReport {
  id: string;
  status: 'confirmed' | 'complete';
  finalAmount?: number;
}

export const getTransactionConfirmation = wf.defineUpdate<TransactionReport>('getTransactionConfirmation');

export async function transactionWorkflow(transactionID: string): Promise<TransactionReport> {
  let confirmed = false;
  wf.setHandler(getTransactionConfirmation, async (): Promise<TransactionReport> => {
    await wf.condition(() => confirmed);
    return { id: transactionID, status: 'confirmed' };
  });
  // Simulate low-latency operation required to initially confirm / authorize the transaction
  await wf.sleep(500);
  confirmed = true;
  // Simulate slower transaction completion / payment capture process
  await wf.sleep(2000);
  return { id: transactionID, status: 'complete', finalAmount: 77 };
}

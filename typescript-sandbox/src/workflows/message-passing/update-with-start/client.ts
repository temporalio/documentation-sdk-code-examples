import { Client, WithStartWorkflowOperation } from '@temporalio/client';
import { getTransactionConfirmation, TransactionReport, transactionWorkflow } from './workflows';

export async function runTransaction(
  client: Client,
  workflowId: string,
  transactionID: string,
): Promise<{ earlyConfirmation: TransactionReport; finalReport: TransactionReport }> {
  // @@@SNIPSTART typescript-message-passing-update-with-start
  const startWorkflowOperation = new WithStartWorkflowOperation(transactionWorkflow, {
    workflowId,
    args: [transactionID],
    taskQueue: 'early-return',
    workflowIdConflictPolicy: 'FAIL',
  });

  const earlyConfirmation = await client.workflow.executeUpdateWithStart(getTransactionConfirmation, {
    startWorkflowOperation,
  });

  const wfHandle = await startWorkflowOperation.workflowHandle();
  const finalReport = await wfHandle.result();
  // @@@SNIPEND
  return { earlyConfirmation, finalReport };
}

import * as wf from '@temporalio/workflow';

export async function example(...args: string[]): Promise<string> {
  return `Received ${args.join(', ')}`;
}

export async function yourWorkflow(): Promise<string> {
  return 'done';
}

export async function failingWorkflow(): Promise<string> {
  throw wf.ApplicationFailure.create({ message: 'Payment declined', nonRetryable: true });
}

export async function subscriptionWorkflow(charges: number[]): Promise<string> {
  const totalCharged = charges.reduce((sum, charge) => sum + charge, 0);
  // @@@SNIPSTART typescript-client-return-result
  return 'Completed ' + wf.workflowInfo().workflowId + ', Total Charged: ' + totalCharged;
  // @@@SNIPEND
}

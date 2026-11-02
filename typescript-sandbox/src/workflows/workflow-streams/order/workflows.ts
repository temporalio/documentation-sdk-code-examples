import { proxyActivities } from '@temporalio/workflow';
import type * as activities from './activities';

const { validateOrder, chargePayment, dispatchOrder } = proxyActivities<typeof activities>({
  startToCloseTimeout: '1 minute',
});

// @@@SNIPSTART typescript-workflow-streams-order-workflow
import { WorkflowStream } from '@temporalio/workflow-streams/workflow';

export interface StatusEvent {
  state: string;
  progress?: number;
  detail?: string;
}

export interface OrderInput {
  orderId: string;
}

export async function orderWorkflow(input: OrderInput): Promise<void> {
  const stream = new WorkflowStream();
  const status = stream.topic<StatusEvent>('status');

  status.publish({ state: 'validating', detail: 'checking inventory' });
  await validateOrder(input.orderId);

  status.publish({ state: 'charging', progress: 33, detail: 'authorizing payment' });
  await chargePayment(input.orderId);

  status.publish({ state: 'shipping', progress: 66, detail: 'dispatching to warehouse' });
  await dispatchOrder(input.orderId);

  status.publish({ state: 'completed', progress: 100 });
}
// @@@SNIPEND

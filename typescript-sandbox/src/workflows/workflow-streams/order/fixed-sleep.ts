import { proxyActivities } from '@temporalio/workflow';
import { WorkflowStream } from '@temporalio/workflow-streams/workflow';
import type * as activities from './activities';
import type { OrderInput, StatusEvent } from './workflows';

const { validateOrder, dispatchOrder } = proxyActivities<typeof activities>({
  startToCloseTimeout: '1 minute',
});

export interface ProgressEvent {
  message: string;
}

// @@@SNIPSTART typescript-workflow-streams-fixed-sleep
import { sleep } from '@temporalio/workflow';

export async function orderWorkflowWithSleep(input: OrderInput): Promise<string> {
  const stream = new WorkflowStream();
  const status = stream.topic<StatusEvent>('status');
  const progress = stream.topic<ProgressEvent>('progress');

  status.publish({ state: 'validating', detail: 'checking inventory' });
  await validateOrder(input.orderId);
  progress.publish({ message: 'inventory reserved' });

  status.publish({ state: 'shipping', progress: 50, detail: 'dispatching to warehouse' });
  await dispatchOrder(input.orderId);
  const result = `order ${input.orderId} shipped`;

  // at the end of the workflow function
  status.publish({ state: 'completed', progress: 100 });
  await sleep('30 seconds');
  return result;
}
// @@@SNIPEND

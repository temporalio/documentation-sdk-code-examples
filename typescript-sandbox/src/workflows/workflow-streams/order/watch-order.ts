// @@@SNIPSTART typescript-workflow-streams-subscribe
import { Client } from '@temporalio/client';
import { WorkflowStreamClient } from '@temporalio/workflow-streams/client';
import type { StatusEvent } from './workflows';

export async function watchOrder(temporalClient: Client, orderId: string): Promise<void> {
  const stream = WorkflowStreamClient.create(temporalClient, orderId);

  const status = stream.topic<StatusEvent>('status');
  for await (const item of status.subscribe()) {
    const evt = item.data;
    console.log(`[${(evt.progress ?? 0).toString().padStart(3)}%] ${evt.state}: ${evt.detail ?? ''}`);
    if (evt.state === 'completed') break;
  }
}
// @@@SNIPEND

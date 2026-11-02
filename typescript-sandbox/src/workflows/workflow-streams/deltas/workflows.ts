import { condition, defineSignal, proxyActivities, setHandler } from '@temporalio/workflow';
import { WorkflowStream } from '@temporalio/workflow-streams/workflow';
import type * as activities from './activities';

const { streamDeltas, streamDeltasWithForceFlush, streamInPhases } = proxyActivities<typeof activities>({
  startToCloseTimeout: '1 minute',
  heartbeatTimeout: '10 seconds',
});

export const doneSignal = defineSignal('done');

// Hosts the stream while an Activity publishes to it, then waits for the subscriber to finish
export async function deltasWorkflow(
  orderId: string,
  publisher: 'streamDeltas' | 'streamDeltasWithForceFlush' | 'streamInPhases',
): Promise<void> {
  new WorkflowStream();
  let done = false;
  setHandler(doneSignal, () => void (done = true));

  if (publisher === 'streamDeltas') await streamDeltas(orderId);
  else if (publisher === 'streamDeltasWithForceFlush') await streamDeltasWithForceFlush(orderId);
  else await streamInPhases();

  await condition(() => done, '30 seconds');
}

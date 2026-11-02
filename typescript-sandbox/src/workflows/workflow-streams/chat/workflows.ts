// @@@SNIPSTART typescript-workflow-streams-llm-workflow
import { condition, defineSignal, proxyActivities, setHandler } from '@temporalio/workflow';
import { WorkflowStream } from '@temporalio/workflow-streams/workflow';
import type * as activities from './activities';

const { streamCompletion } = proxyActivities<typeof activities>({
  startToCloseTimeout: '5 minutes',
});

export const subscriberAcknowledgedTerminator = defineSignal('subscriberAcknowledgedTerminator');

export interface ChatInput {
  prompt: string;
}

export async function chatWorkflow(input: ChatInput): Promise<string> {
  const stream = new WorkflowStream();
  let subscriberDone = false;
  setHandler(subscriberAcknowledgedTerminator, () => {
    subscriberDone = true;
  });

  const result = await streamCompletion(input.prompt);

  // Wait for the subscriber to ack the terminal `close` event. The timeout
  // is a fallback for when no subscriber is attached; with the ack, the
  // typical case exits as soon as the subscriber confirms.
  await condition(() => subscriberDone, '30 seconds');
  // Returns true if the ack arrived, false on timeout. Either way, fall through.
  return result;
}
// @@@SNIPEND

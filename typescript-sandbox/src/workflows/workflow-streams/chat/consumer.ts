// @@@SNIPSTART typescript-workflow-streams-llm-consumer
import { Client } from '@temporalio/client';
import { defaultPayloadConverter } from '@temporalio/common';
import { WorkflowStreamClient } from '@temporalio/workflow-streams/client';
import type { TextDelta } from './activities';
import { subscriberAcknowledgedTerminator } from './workflows';

export async function streamChat(temporalClient: Client, chatId: string): Promise<string> {
  // Subscribe-only; no `await using` needed because the flusher only runs for publishers.
  const stream = WorkflowStreamClient.create(temporalClient, chatId);
  const output: string[] = [];

  function render(): void {
    // ... display the accumulated output (terminal redraw, UI update, etc.)
  }

  for await (const item of stream.subscribe(['delta', 'retry', 'close'])) {
    if (item.topic === 'retry') {
      // Earlier attempt's deltas are stale; drop what we've shown.
      output.length = 0;
      render();
    } else if (item.topic === 'delta') {
      const delta = defaultPayloadConverter.fromPayload<TextDelta>(item.data);
      output.push(delta.text);
      render();
    } else if (item.topic === 'close') {
      // Acknowledge so the Workflow can return without waiting on the fallback timeout.
      await temporalClient.workflow.getHandle(chatId).signal(subscriberAcknowledgedTerminator);
      break;
    }
  }

  return output.join('');
}
// @@@SNIPEND

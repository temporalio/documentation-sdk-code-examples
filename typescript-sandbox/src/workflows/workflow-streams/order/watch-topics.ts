import { Client } from '@temporalio/client';
import { WorkflowStreamClient } from '@temporalio/workflow-streams/client';
import type { ProgressEvent } from './fixed-sleep';
import type { StatusEvent } from './workflows';

// @@@SNIPSTART typescript-workflow-streams-heterogeneous-topics
import { defaultPayloadConverter } from '@temporalio/common';

export async function watchStatusAndProgress(temporalClient: Client, workflowId: string): Promise<void> {
  const stream = WorkflowStreamClient.create(temporalClient, workflowId);

  for await (const item of stream.subscribe(['status', 'progress'])) {
    if (item.topic === 'status') {
      const evt = defaultPayloadConverter.fromPayload<StatusEvent>(item.data);
      console.log(`[status] ${evt.state}: ${evt.detail ?? ''}`);
    } else if (item.topic === 'progress') {
      const evt = defaultPayloadConverter.fromPayload<ProgressEvent>(item.data);
      console.log(`[progress] ${evt.message}`);
    }
  }
}
// @@@SNIPEND

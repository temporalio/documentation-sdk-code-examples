// @@@SNIPSTART typescript-workflow-streams-publish-from-client
import { Client } from '@temporalio/client';
import { WorkflowStreamClient } from '@temporalio/workflow-streams/client';
import type { StatusEvent } from './workflows';

export async function publishStatus(temporalClient: Client, workflowId: string): Promise<void> {
  await using streamClient = WorkflowStreamClient.create(temporalClient, workflowId, {
    batchInterval: '200 milliseconds',
  });

  const status = streamClient.topic<StatusEvent>('status');
  status.publish({ state: 'started' });
  // ...
  // Buffer is flushed automatically on `await using` scope exit.
}
// @@@SNIPEND

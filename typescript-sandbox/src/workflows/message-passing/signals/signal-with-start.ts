// @@@SNIPSTART typescript-message-passing-signal-with-start
import { Client } from '@temporalio/client';
import { joinSignal, yourWorkflow } from './other-workflow';

export async function signalWithStart(client: Client) {
  await client.workflow.signalWithStart(yourWorkflow, {
    workflowId: 'workflow-id-123',
    taskQueue: 'my-taskqueue',
    args: [{ foo: 1 }],
    signal: joinSignal,
    signalArgs: [{ userId: 'user-1', groupId: 'group-1' }],
  });
}
// @@@SNIPEND

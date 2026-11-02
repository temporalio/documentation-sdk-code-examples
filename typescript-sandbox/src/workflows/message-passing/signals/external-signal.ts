// @@@SNIPSTART typescript-message-passing-external-signal
import { getExternalWorkflowHandle } from '@temporalio/workflow';
import { joinSignal } from './other-workflow';

export async function yourWorkflowThatSignals() {
  const handle = getExternalWorkflowHandle('workflow-id-123');
  await handle.signal(joinSignal, { userId: 'user-1', groupId: 'group-1' });
}
// @@@SNIPEND

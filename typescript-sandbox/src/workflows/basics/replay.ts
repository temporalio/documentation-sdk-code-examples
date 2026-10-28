// @@@SNIPSTART typescript-workflow-basics-replay
import { log, workflowInfo } from '@temporalio/workflow';

// Stand-in for your own metrics client
const metrics = {
  emit(name: string, value: number): void {
    log.info('Emitting metric', { name, value });
  },
};

export async function replayAwareWorkflow(): Promise<void> {
  if (!workflowInfo().unsafe.isReplaying) {
    metrics.emit('workflow_started', 1);
  }
}
// @@@SNIPEND

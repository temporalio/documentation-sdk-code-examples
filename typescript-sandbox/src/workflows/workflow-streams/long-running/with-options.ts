import { workflowInfo } from '@temporalio/workflow';
import { WorkflowStream, type WorkflowStreamState } from '@temporalio/workflow-streams/workflow';
import { doOneIteration } from './workflows';

export interface WorkflowInput {
  itemsProcessed: number;
  streamState?: WorkflowStreamState;
}

// @@@SNIPSTART typescript-workflow-streams-continue-as-new-options
import { allHandlersFinished, condition, makeContinueAsNewFunc } from '@temporalio/workflow';

export async function longRunningWorkflow(input: WorkflowInput): Promise<void> {
  const stream = new WorkflowStream(input.streamState);
  let itemsProcessed = input.itemsProcessed;

  while (true) {
    await doOneIteration(stream);
    itemsProcessed++;

    if (workflowInfo().continueAsNewSuggested) {
      stream.detachPollers();
      await condition(allHandlersFinished);
      const continueWithOptions = makeContinueAsNewFunc<typeof longRunningWorkflow>({
        taskQueue: 'other-tq',
      });
      await continueWithOptions({
        itemsProcessed,
        streamState: stream.getState(),
      });
    }
  }
}
// @@@SNIPEND

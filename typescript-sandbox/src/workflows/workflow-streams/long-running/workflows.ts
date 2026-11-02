import { sleep } from '@temporalio/workflow';

// @@@SNIPSTART typescript-workflow-streams-continue-as-new
import { workflowInfo } from '@temporalio/workflow';
import { WorkflowStream, type WorkflowStreamState } from '@temporalio/workflow-streams/workflow';

export interface WorkflowInput {
  itemsProcessed: number;
  streamState?: WorkflowStreamState;
}

export async function longRunningWorkflow(input: WorkflowInput): Promise<void> {
  const stream = new WorkflowStream(input.streamState);
  let itemsProcessed = input.itemsProcessed;

  while (true) {
    await doOneIteration(stream);
    itemsProcessed++;

    if (workflowInfo().continueAsNewSuggested) {
      await stream.continueAsNew<typeof longRunningWorkflow>((state) => [
        {
          itemsProcessed,
          streamState: state,
        },
      ]);
    }
  }
}
// @@@SNIPEND

export async function doOneIteration(stream: WorkflowStream): Promise<void> {
  stream.topic<{ tick: number }>('tick').publish({ tick: Date.now() });
  await sleep('100 milliseconds');
}

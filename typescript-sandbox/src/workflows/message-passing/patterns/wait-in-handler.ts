import * as wf from '@temporalio/workflow';
import { MyUpdateInput, MyUpdateOutput, myUpdate } from './shared';

export async function waitInHandlerWorkflow(): Promise<void> {
  // @@@SNIPSTART typescript-message-passing-wait-in-handler
  let readyForUpdateToExecute = false;

  wf.setHandler(myUpdate, async (input: MyUpdateInput): Promise<MyUpdateOutput> => {
    await wf.condition(() => readyForUpdateToExecute);
    return { greeting: `Hello, ${input.name}!` };
  });
  // @@@SNIPEND

  // Simulate initialization work that must finish before the Update handler runs
  await wf.sleep('1 second');
  readyForUpdateToExecute = true;
  await wf.condition(wf.allHandlersFinished);
}

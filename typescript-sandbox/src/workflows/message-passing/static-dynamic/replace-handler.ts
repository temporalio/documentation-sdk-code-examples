import * as wf from '@temporalio/workflow';

export const MySignal = wf.defineSignal('MySignal');
export const handledByQuery = wf.defineQuery<string>('handledBy');

export async function replaceHandlerWorkflow(): Promise<string> {
  let handledBy = '';
  const handlerFn1 = () => void (handledBy = 'handlerFn1');
  const handlerFn2 = () => void (handledBy = 'handlerFn2');
  wf.setHandler(handledByQuery, () => handledBy);

  // @@@SNIPSTART typescript-message-passing-replace-handler
  wf.setHandler(MySignal, handlerFn1);
  wf.setHandler(MySignal, handlerFn2); // replaces handlerFn1
  // @@@SNIPEND

  await wf.condition(() => handledBy !== '');
  return handledBy;
}

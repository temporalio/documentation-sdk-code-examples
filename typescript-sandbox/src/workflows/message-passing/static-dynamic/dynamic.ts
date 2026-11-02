import * as wf from '@temporalio/workflow';

export const handledQuery = wf.defineQuery<string[]>('handled');

interface TaskPayload {
  taskId: string;
}

export async function dynamicSignalsWorkflow(taskAId: string, taskBId: string): Promise<string[]> {
  const handled: string[] = [];
  wf.setHandler(handledQuery, () => handled);

  // @@@SNIPSTART typescript-message-passing-dynamic-definitions
  // "fat handler" solution
  wf.setHandler(wf.defineSignal<[TaskPayload]>('genericSignal'), (payload) => {
    switch (payload.taskId) {
      case taskAId:
        handled.push(`genericSignal: task A`); // do task A things
        break;
      case taskBId:
        handled.push(`genericSignal: task B`); // do task B things
        break;
      default:
        throw new Error('Unexpected task.');
    }
  });

  // "inline definition" solution
  wf.setHandler(wf.defineSignal<[TaskPayload]>(`task-${taskAId}`), (payload) => {
    handled.push(`inline: ${payload.taskId}`); // do task A things
  });
  wf.setHandler(wf.defineSignal<[TaskPayload]>(`task-${taskBId}`), (payload) => {
    handled.push(`inline: ${payload.taskId}`); // do task B things
  });

  // utility "inline definition" helper
  const inlineSignal = (signalName: string, handler: (payload: TaskPayload) => void) =>
    wf.setHandler(wf.defineSignal<[TaskPayload]>(signalName), handler);
  inlineSignal(`task-${taskBId}`, (payload) => {
    handled.push(`helper: ${payload.taskId}`); // do task B things
  });
  // @@@SNIPEND

  await wf.condition(() => handled.length >= 3);
  return handled;
}

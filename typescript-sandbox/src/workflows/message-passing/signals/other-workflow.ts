import * as wf from '@temporalio/workflow';

export interface JoinInput {
  userId: string;
  groupId: string;
}

export const joinSignal = wf.defineSignal<[JoinInput]>('join');

export async function yourWorkflow(input: { foo: number }): Promise<JoinInput[]> {
  const joined: JoinInput[] = [];
  wf.setHandler(joinSignal, (join) => void joined.push(join));
  await wf.condition(() => joined.length >= input.foo);
  return joined;
}

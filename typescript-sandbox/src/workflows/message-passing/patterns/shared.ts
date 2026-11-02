import * as wf from '@temporalio/workflow';

export interface MyWorkflowOutput {
  x: number;
  y: number;
}

export interface MyUpdateInput {
  name: string;
}

export interface MyUpdateOutput {
  greeting: string;
}

export const mySignal = wf.defineSignal('mySignal');
export const myUpdate = wf.defineUpdate<MyUpdateOutput, [MyUpdateInput]>('myUpdate');
export const finishSignal = wf.defineSignal('finish');

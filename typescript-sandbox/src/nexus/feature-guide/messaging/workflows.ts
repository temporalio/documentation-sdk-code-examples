import * as wf from '@temporalio/workflow';
import { Language } from './api';

export const getLanguagesQuery = wf.defineQuery<Language[], []>('getLanguages');
export const approveSignal = wf.defineSignal<[]>('approve');

// A long-running entity Workflow for one user, reached through the Nexus Service
export async function greetingWorkflow(): Promise<string> {
  const greetings: Partial<Record<Language, string>> = { chinese: '你好，世界', english: 'Hello, world' };
  let approved = false;
  wf.setHandler(getLanguagesQuery, () => Object.keys(greetings) as Language[]);
  wf.setHandler(approveSignal, () => void (approved = true));
  await wf.condition(() => approved);
  return greetings.english!;
}

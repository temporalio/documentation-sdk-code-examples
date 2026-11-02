import { log } from '@temporalio/workflow';

export async function reminder(text: string): Promise<void> {
  log.info(`reminder: ${text}`);
}

export async function scheduledWorkflow(): Promise<void> {
  log.info('scheduledWorkflow ran');
}

export async function someWorkflow(): Promise<void> {
  log.info('someWorkflow ran');
}

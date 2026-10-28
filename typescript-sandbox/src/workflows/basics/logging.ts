// @@@SNIPSTART typescript-workflow-basics-logging
import { log } from '@temporalio/workflow';

export async function myWorkflow(name: string): Promise<string> {
  log.info('Starting workflow', { name });
  return `Hello, ${name}!`;
}
// @@@SNIPEND

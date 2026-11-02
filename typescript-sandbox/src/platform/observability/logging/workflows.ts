// @@@SNIPSTART typescript-observability-workflow-logger
import { log } from '@temporalio/workflow';

export async function myWorkflow(name: string): Promise<string> {
  log.info('Log from workflow', { name });
  return `Hello, ${name}!`;
}
// @@@SNIPEND

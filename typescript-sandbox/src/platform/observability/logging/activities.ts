// @@@SNIPSTART typescript-observability-activity-logger
import { log } from '@temporalio/activity';

export async function greet(name: string): Promise<string> {
  log.info('Log from activity', { name });
  return `Hello, ${name}!`;
}
// @@@SNIPEND

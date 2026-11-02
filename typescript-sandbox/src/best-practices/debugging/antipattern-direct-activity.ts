// Antipattern: the Worker can't bundle this file as Workflow code
// @@@SNIPSTART typescript-debugging-antipattern-direct-activity
import { makeHTTPRequest } from './activities';

export async function yourWorkflow(): Promise<string> {
  return await makeHTTPRequest('https://temporal.io');
}
// @@@SNIPEND

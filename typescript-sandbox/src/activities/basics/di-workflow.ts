// @@@SNIPSTART typescript-activity-basics-dependency-injection-workflow
import { proxyActivities } from '@temporalio/workflow';
import type { createActivities } from './di-activities';

// Note usage of ReturnType<> generic since createActivities is a factory function
const { greet, greet_es } = proxyActivities<ReturnType<typeof createActivities>>({
  startToCloseTimeout: '30 seconds',
});
// @@@SNIPEND

export async function dependencyInjectionWorkflow(): Promise<string[]> {
  return [await greet('Hello'), await greet_es('Hola')];
}

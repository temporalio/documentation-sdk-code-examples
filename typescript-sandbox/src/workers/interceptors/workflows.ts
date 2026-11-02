import { createNexusServiceClient, proxyActivities } from '@temporalio/workflow';
import type * as activities from './activities';
import { echoService } from './nexus-service';

const { greet } = proxyActivities<typeof activities>({
  startToCloseTimeout: '1 minute',
});

export async function greetingWorkflow(name: string): Promise<string> {
  return await greet(name);
}

export async function echoCallerWorkflow(endpoint: string, message: string): Promise<string> {
  const echo = createNexusServiceClient({ service: echoService, endpoint });
  return await echo.executeOperation('echo', message, { scheduleToCloseTimeout: '10s' });
}

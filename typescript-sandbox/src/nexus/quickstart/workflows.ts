// @@@SNIPSTART typescript-nexus-quickstart-workflows
import { proxyActivities, createNexusServiceClient } from '@temporalio/workflow';
// Only import the activity types
import type * as activities from './activities';
import { sayHelloService } from './service';

const { greet } = proxyActivities<typeof activities>({
  startToCloseTimeout: '1 minute',
});

/** A workflow that simply calls an activity */
export async function example(name: string): Promise<string> {
  return await greet(name);
}

const NEXUS_ENDPOINT = 'my-nexus-endpoint-name';

export async function callerWorkflow(name: string): Promise<string> {
  const nexusClient = createNexusServiceClient({
    service: sayHelloService,
    endpoint: NEXUS_ENDPOINT,
  });
  return await nexusClient.executeOperation('sayHello', { name }, { scheduleToCloseTimeout: '10s' });
}
// @@@SNIPEND

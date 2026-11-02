import * as wf from '@temporalio/workflow';
import { nexusGreetingService, GetLanguagesOutput } from './api';

export async function getLanguagesCallerWorkflow(endpoint: string, userId: string): Promise<GetLanguagesOutput> {
  const nexusClient = wf.createNexusServiceClient({ service: nexusGreetingService, endpoint });
  return await nexusClient.executeOperation('getLanguages', { userId }, { scheduleToCloseTimeout: '10s' });
}

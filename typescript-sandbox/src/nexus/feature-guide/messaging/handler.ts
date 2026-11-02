import * as nexus from 'nexus-rpc';
import * as temporalNexus from '@temporalio/nexus';
import { nexusGreetingService, GetLanguagesInput } from './api';
import { getLanguagesQuery } from './workflows';

// @@@SNIPSTART typescript-nexus-feature-guide-messaging
function workflowIdForUser(userId: string): string {
  return `GreetingWorkflow_for_${userId}`;
}

export const nexusGreetingServiceHandler = nexus.serviceHandler(nexusGreetingService, {
  getLanguages: new temporalNexus.TemporalOperationHandler({
    async start(_ctx, client, input: GetLanguagesInput) {
      const handle = client.client.workflow.getHandle(workflowIdForUser(input.userId));
      const result = await handle.query(getLanguagesQuery);
      return temporalNexus.TemporalOperationResult.sync(result);
    },
  }),
});
// @@@SNIPEND

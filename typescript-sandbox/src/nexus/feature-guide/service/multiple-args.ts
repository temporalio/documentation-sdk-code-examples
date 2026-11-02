import * as nexus from 'nexus-rpc';
import * as temporalNexus from '@temporalio/nexus';
import { helloArgsService, HelloInput, HelloOutput } from '../api';
import { helloWorkflowWithArgs } from './workflows';

export const helloArgsServiceHandler = nexus.serviceHandler(helloArgsService, {
  hello: new temporalNexus.TemporalOperationHandler<HelloInput, HelloOutput>({
    start: async (_ctx, client, input) =>
      // @@@SNIPSTART typescript-nexus-feature-guide-multiple-args
      client.startWorkflow(helloWorkflowWithArgs, {
        args: [input.name, input.language],
        workflowId: `hello-${input.name}-${input.language}`,
      }),
    // @@@SNIPEND
  }),
});

// @@@SNIPSTART typescript-nexus-feature-guide-handler
import * as nexus from 'nexus-rpc';
import * as temporalNexus from '@temporalio/nexus';
import { helloService, EchoInput, EchoOutput, HelloInput, HelloOutput } from '../api';
import { helloWorkflow } from './workflows';

export const helloServiceHandler = nexus.serviceHandler(helloService, {
  echo: new temporalNexus.TemporalOperationHandler<EchoInput, EchoOutput>({
    start: async (ctx, client, input) => {
      return temporalNexus.TemporalOperationResult.sync({ message: input.message });
    },
  }),
  hello: new temporalNexus.TemporalOperationHandler<HelloInput, HelloOutput>({
    start: async (ctx, client, input) =>
      client.startWorkflow(helloWorkflow, {
        args: [input],

        // Workflow IDs should typically be business-meaningful IDs and are used to dedupe workflow starts.
        workflowId: `hello-${input.name}-${input.language}`,

        // Task queue defaults to the task queue this Operation is handled on.
      }),
  }),
});
// @@@SNIPEND

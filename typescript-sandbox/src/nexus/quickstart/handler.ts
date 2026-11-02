// @@@SNIPSTART typescript-nexus-quickstart-handler
import { randomUUID } from 'crypto';
import * as nexus from 'nexus-rpc';
import * as temporalNexus from '@temporalio/nexus';
import { sayHelloService, MyInput } from './service';
import { example } from './workflows';

export const sayHelloHandler = nexus.serviceHandler(sayHelloService, {
  sayHello: new temporalNexus.WorkflowRunOperationHandler<MyInput, string>(async (ctx, input: MyInput) => {
    return await temporalNexus.startWorkflow(ctx, example, {
      args: [input.name],
      workflowId: 'say-hello-nexus-' + randomUUID(),

      // Task queue defaults to the task queue this Operation is handled on.
    });
  }),
});
// @@@SNIPEND

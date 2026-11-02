// @@@SNIPSTART typescript-interceptors-nexus-log
import type {
  NexusInboundCallsInterceptor,
  NexusStartOperationInput,
  NexusStartOperationOutput,
  Next,
} from '@temporalio/worker';

export class NexusOperationLogInterceptor implements NexusInboundCallsInterceptor {
  async startOperation(
    input: NexusStartOperationInput,
    next: Next<NexusInboundCallsInterceptor, 'startOperation'>,
  ): Promise<NexusStartOperationOutput> {
    console.log('Starting Nexus operation', {
      service: input.ctx.service,
      operation: input.ctx.operation,
    });
    const output = await next(input);
    console.log('Nexus operation started', {
      service: input.ctx.service,
      operation: input.ctx.operation,
      async: output.result.isAsync,
    });
    return output;
  }
}
// @@@SNIPEND

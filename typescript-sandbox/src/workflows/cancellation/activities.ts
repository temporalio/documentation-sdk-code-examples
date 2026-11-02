import { CancelledFailure, Context } from '@temporalio/activity';

export const cleanup = { ran: false };

export async function processOrder(orderId: string): Promise<string> {
  try {
    for (let step = 0; step < 600; step++) {
      // Heartbeating is how the Activity learns that it was cancelled
      Context.current().heartbeat(step);
      await Context.current().sleep('100 milliseconds');
    }
  } catch (err) {
    if (err instanceof CancelledFailure) {
      // Clean up, then re-throw so the Activity shows as cancelled
      cleanup.ran = true;
      throw err;
    }
    throw err;
  }
  return orderId;
}

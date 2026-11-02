// @@@SNIPSTART typescript-workflow-streams-from-within-activity
import { Context } from '@temporalio/activity';
import { WorkflowStreamClient } from '@temporalio/workflow-streams/client';

export interface Delta {
  text: string;
}

export async function streamDeltas(orderId: string): Promise<void> {
  await using client = WorkflowStreamClient.fromWithinActivity();
  const deltas = client.topic<Delta>('delta');

  for await (const delta of generateDeltas(orderId)) {
    deltas.publish(delta);
    Context.current().heartbeat();
  }
  // Buffer is flushed automatically on scope exit.
}
// @@@SNIPEND

export async function streamDeltasWithForceFlush(orderId: string): Promise<void> {
  await using client = WorkflowStreamClient.fromWithinActivity();
  const deltas = client.topic<Delta>('delta');

  for await (const delta of generateDeltas(orderId)) {
    // @@@SNIPSTART typescript-workflow-streams-force-flush
    deltas.publish(delta, { forceFlush: true });
    // @@@SNIPEND
  }
}

export async function streamInPhases(): Promise<string> {
  // @@@SNIPSTART typescript-workflow-streams-flush
  await using client = WorkflowStreamClient.fromWithinActivity();
  const deltas = client.topic<Delta>('delta');

  for (const delta of firstPhase()) {
    deltas.publish(delta);
  }

  await client.flush();
  const checkpointId = await recordPhaseOneComplete(); // only safe once phase-one events are durable

  for (const delta of secondPhase(checkpointId)) {
    deltas.publish(delta);
  }
  // @@@SNIPEND
  return checkpointId;
}

// Simulates text arriving in pieces, for example from a model
async function* generateDeltas(orderId: string): AsyncGenerator<Delta> {
  for (const text of [`Order ${orderId} `, 'is ', 'on ', 'its ', 'way.']) {
    await new Promise((resolve) => setTimeout(resolve, 50));
    yield { text };
  }
}

function firstPhase(): Delta[] {
  return [{ text: 'phase one, ' }, { text: 'part two. ' }];
}

async function recordPhaseOneComplete(): Promise<string> {
  return 'checkpoint-1';
}

function secondPhase(checkpointId: string): Delta[] {
  return [{ text: `phase two after ${checkpointId}.` }];
}

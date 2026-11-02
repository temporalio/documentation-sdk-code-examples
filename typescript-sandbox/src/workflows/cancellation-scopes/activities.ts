import { Context } from '@temporalio/activity';

export const cleanupCalls: string[] = [];

// Simulates a request: URLs containing "slow" take 2 seconds, others 100 milliseconds.
// Heartbeating lets the Activity learn that it was cancelled.
async function simulateRequest(url: string): Promise<void> {
  const durationMs = url.includes('slow') ? 2000 : 100;
  for (let elapsed = 0; elapsed < durationMs; elapsed += 50) {
    Context.current().heartbeat();
    await Context.current().sleep(50);
  }
}

export async function httpGetJSON(url: string): Promise<{ url: string }> {
  await simulateRequest(url);
  return { url };
}

export async function httpPostJSON(url: string, _data: unknown): Promise<void> {
  await simulateRequest(url);
}

export async function setup(): Promise<void> {}

export async function cleanup(url: string): Promise<void> {
  cleanupCalls.push(url);
}

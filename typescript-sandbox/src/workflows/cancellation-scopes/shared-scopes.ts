import { CancellationScope, proxyActivities } from '@temporalio/workflow';
import type * as activities from './activities';

const { httpGetJSON } = proxyActivities<typeof activities>({
  startToCloseTimeout: '10m',
});

// @@@SNIPSTART typescript-cancellation-scopes-non-cancellable
export async function nonCancellable(url: string): Promise<any> {
  // Prevent Activity from being cancelled and await completion.
  // Note that the Workflow is completely oblivious and impervious to cancellation in this example.
  return CancellationScope.nonCancellable(() => httpGetJSON(url));
}
// @@@SNIPEND

// @@@SNIPSTART typescript-cancellation-scopes-shared-promises
export async function sharedScopes(): Promise<any> {
  // Start activities in the root scope
  const p1 = httpGetJSON('http://url1.ninja');
  const p2 = httpGetJSON('http://url2.ninja');

  const scopePromise = CancellationScope.cancellable(async () => {
    const first = await Promise.race([p1, p2]);
    // Does not cancel activity1 or activity2 as they're linked to the root scope
    CancellationScope.current().cancel();
    return first;
  });
  return await scopePromise;
  // The Activity that did not complete will effectively be cancelled when
  // Workflow completes unless the Activity is awaited:
  // await Promise.all([p1, p2]);
}
// @@@SNIPEND

// @@@SNIPSTART typescript-cancellation-scopes-shield-awaited-in-root
export async function shieldAwaitedInRootScope(): Promise<any> {
  let p: Promise<any> | undefined = undefined;

  await CancellationScope.nonCancellable(async () => {
    p = httpGetJSON('http://example.com'); // <-- Start activity in nonCancellable scope without awaiting completion
  });
  // Activity is shielded from cancellation even though it is awaited in the cancellable root scope
  return p;
}
// @@@SNIPEND

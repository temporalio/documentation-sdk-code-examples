// @@@SNIPSTART typescript-testing-cancel-activity
import { CancelledFailure, heartbeat, sleep } from '@temporalio/activity';
import { MockActivityEnvironment } from '@temporalio/testing';
import assert from 'assert';

async function activityFoo(): Promise<void> {
  heartbeat(6);
  // @temporalio/activity's sleep() is Cancellation-aware, which means that on Cancellation,
  // CancelledFailure will be thrown from it.
  await sleep(100);
}

it('throws CancelledFailure when the Activity is canceled', async () => {
  const env = new MockActivityEnvironment();

  env.on('heartbeat', (d: unknown) => {
    assert(d === 6);
    // Cancel the Activity once it has sent its Heartbeat
    env.cancel();
  });

  await assert.rejects(env.run(activityFoo), (err) => {
    assert.ok(err instanceof CancelledFailure);
    return true;
  });
});
// @@@SNIPEND

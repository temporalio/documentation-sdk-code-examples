// @@@SNIPSTART typescript-testing-heartbeat
import { heartbeat } from '@temporalio/activity';
import { MockActivityEnvironment } from '@temporalio/testing';
import assert from 'assert';

async function activityFoo(): Promise<void> {
  heartbeat(6);
}

it('heartbeats with the expected details', async () => {
  const env = new MockActivityEnvironment();

  env.on('heartbeat', (d: unknown) => {
    assert(d === 6);
  });

  await env.run(activityFoo);
});
// @@@SNIPEND

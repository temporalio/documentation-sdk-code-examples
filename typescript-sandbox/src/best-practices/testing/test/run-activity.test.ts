// @@@SNIPSTART typescript-testing-run-activity
import { activityInfo } from '@temporalio/activity';
import { MockActivityEnvironment } from '@temporalio/testing';
import assert from 'assert';

// A function that takes two numbers and returns a promise that resolves to the sum of the two numbers
// and the current attempt.
async function activityFoo(a: number, b: number): Promise<number> {
  return a + b + activityInfo().attempt;
}

it('adds the current attempt to the sum', async () => {
  // Create a MockActivityEnvironment with attempt set to 2. Run the activityFoo
  // function with parameters 5 and 35. Assert that the result is 42.
  const env = new MockActivityEnvironment({ attempt: 2 });
  const result = await env.run(activityFoo, 5, 35);
  assert.equal(result, 42);
});
// @@@SNIPEND

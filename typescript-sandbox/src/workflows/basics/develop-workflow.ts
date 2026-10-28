import { proxyActivities } from '@temporalio/workflow';
import type * as activities from '../../activities';

const { greet } = proxyActivities<typeof activities>({
  startToCloseTimeout: '1 minute',
});

// @@@SNIPSTART typescript-workflow-basics-develop
type ExampleArgs = {
  name: string;
};

export async function example(args: ExampleArgs): Promise<{ greeting: string }> {
  const greeting = await greet(args.name);
  return { greeting };
}
// @@@SNIPEND

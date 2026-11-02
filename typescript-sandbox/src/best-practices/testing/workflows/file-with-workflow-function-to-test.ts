// @@@SNIPSTART typescript-testing-function-to-test
import { sleep } from '@temporalio/workflow';

export async function functionToTest(): Promise<number> {
  await sleep('1 day');
  return 42;
}
// @@@SNIPEND

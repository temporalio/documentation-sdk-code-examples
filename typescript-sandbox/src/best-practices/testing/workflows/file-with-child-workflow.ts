// @@@SNIPSTART typescript-testing-function-with-child
import * as wf from '@temporalio/workflow';
import { someWorkflowToRunAsChild } from './some-workflow';

export { someWorkflowToRunAsChild };

export async function functionToTest(): Promise<number> {
  const result = await wf.executeChild(someWorkflowToRunAsChild);
  return result + 42;
}
// @@@SNIPEND

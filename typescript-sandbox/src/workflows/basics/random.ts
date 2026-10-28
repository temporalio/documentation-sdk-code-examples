// @@@SNIPSTART typescript-workflow-basics-random
import { uuid4 } from '@temporalio/workflow';

export async function randomValues(): Promise<{ value: number; id: string }> {
  const value = Math.random();
  const id = uuid4();
  return { value, id };
}
// @@@SNIPEND

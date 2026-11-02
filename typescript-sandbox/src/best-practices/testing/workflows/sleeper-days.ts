// @@@SNIPSTART typescript-testing-sleeper-days-workflow
import { sleep } from '@temporalio/workflow';
import { defineQuery, setHandler } from '@temporalio/workflow';

export const daysQuery = defineQuery<number>('days');

export async function sleeperWorkflow() {
  let numDays = 0;

  setHandler(daysQuery, () => numDays);

  for (let i = 0; i < 100; i++) {
    await sleep('1 day');
    numDays++;
  }
}
// @@@SNIPEND

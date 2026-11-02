// @@@SNIPSTART typescript-testing-sleeper-workflow
import { sleep } from '@temporalio/workflow';

export async function sleeperWorkflow() {
  await sleep('1 day');
}
// @@@SNIPEND

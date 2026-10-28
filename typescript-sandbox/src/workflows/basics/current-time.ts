// @@@SNIPSTART typescript-workflow-basics-current-time
import { sleep } from '@temporalio/workflow';

export async function currentTime(): Promise<void> {
  // Prints the *exact* same timestamp on every iteration
  for (let x = 0; x < 10; ++x) {
    console.log(Date.now());
  }

  // Prints timestamps increasing roughly 1s each iteration
  for (let x = 0; x < 10; ++x) {
    await sleep('1 second');
    console.log(Date.now());
  }
}
// @@@SNIPEND

// @@@SNIPSTART typescript-run-worker-bundle-workflow
import { bundleWorkflowCode } from '@temporalio/worker';
import { writeFile } from 'fs/promises';
import path from 'path';

export async function bundle() {
  const { code } = await bundleWorkflowCode({
    workflowsPath: require.resolve('../workflows'),
  });
  const codePath = path.join(__dirname, '../workflow-bundle.js');

  await writeFile(codePath, code);
  console.log(`Bundle written to ${codePath}`);
}
// @@@SNIPEND

if (require.main === module) {
  bundle().catch((err) => {
    console.error(err);
    process.exit(1);
  });
}

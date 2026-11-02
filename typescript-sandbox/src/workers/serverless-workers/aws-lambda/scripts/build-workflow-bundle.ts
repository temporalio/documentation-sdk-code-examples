// @@@SNIPSTART typescript-aws-lambda-bundle
import { bundleWorkflowCode } from '@temporalio/worker';
import { writeFile } from 'fs/promises';
import path from 'path';

export async function bundle(): Promise<void> {
  const { code } = await bundleWorkflowCode({
    workflowsPath: require.resolve('../workflows'),
  });
  await writeFile(path.join(__dirname, '../workflow-bundle.js'), code);
}
// @@@SNIPEND

if (require.main === module) {
  bundle().catch((err) => {
    console.error(err);
    process.exit(1);
  });
}

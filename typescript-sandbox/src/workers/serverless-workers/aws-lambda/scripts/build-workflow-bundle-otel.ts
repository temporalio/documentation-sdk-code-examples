// @@@SNIPSTART typescript-aws-lambda-bundle-otel
import { bundleWorkflowCode } from '@temporalio/worker';
import { makeOtelPlugin } from '@temporalio/lambda-worker/otel';
import { writeFile } from 'fs/promises';
import path from 'path';

export async function bundle(): Promise<void> {
  const { plugin } = makeOtelPlugin();
  const { code } = await bundleWorkflowCode({
    workflowsPath: require.resolve('../workflows'),
    plugins: [plugin],
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

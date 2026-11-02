import { TestWorkflowEnvironment } from '@temporalio/testing';
import { Worker } from '@temporalio/worker';
import { Client } from '@temporalio/client';
import fs from 'fs';
import os from 'os';
import path from 'path';
import { spawnSync } from 'child_process';
import { workflowFoo } from '../workflows/foo';

describe('Replay a Workflow Execution', () => {
  let testEnv: TestWorkflowEnvironment;
  let client: Client;
  const workflowsPath = require.resolve('../workflows/foo');
  let filePath: string;

  before(async () => {
    // Listing Workflows needs Visibility, which the time-skipping test server doesn't provide
    testEnv = await TestWorkflowEnvironment.createLocal();
    client = testEnv.client;
    const worker = await Worker.create({ connection: testEnv.nativeConnection, taskQueue: 'foo', workflowsPath });
    await worker.runUntil(testEnv.client.workflow.execute(workflowFoo, { taskQueue: 'foo', workflowId: 'your-workflow-id' }));
    // Export the history with the Temporal CLI, as `temporal workflow show --output json` does for readers
    filePath = path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'replay-')), 'history_file.json');
    const show = spawnSync('temporal', ['workflow', 'show', '--address', testEnv.address, '--workflow-id', 'your-workflow-id', '--output', 'json']);
    if (show.status === 0) fs.writeFileSync(filePath, show.stdout);
  });

  after(async () => {
    await testEnv?.teardown();
  });

  it('replays a history loaded from a JSON file', async function () {
    if (!fs.existsSync(filePath)) this.skip(); // The Temporal CLI isn't installed
    // @@@SNIPSTART typescript-testing-replay-from-file
    const history = JSON.parse(await fs.promises.readFile(filePath, 'utf8'));
    await Worker.runReplayHistory(
      {
        workflowsPath: require.resolve('../workflows/foo'),
      },
      history,
    );
    // @@@SNIPEND
  });

  it('replays a history downloaded with a Client', async () => {
    // @@@SNIPSTART typescript-testing-replay-from-client
    const handle = client.workflow.getHandle('your-workflow-id');
    const history = await handle.fetchHistory();
    await Worker.runReplayHistory(
      {
        workflowsPath: require.resolve('../workflows/foo'),
      },
      history,
    );
    // @@@SNIPEND
  });

  it('replays histories in bulk', async () => {
    // Visibility can lag behind the Workflow that just completed
    for (let i = 0; i < 50 && (await client.workflow.count('TaskQueue="foo"')).count === 0; i++) {
      await new Promise((resolve) => setTimeout(resolve, 200));
    }
    // The sample reports replay failures with console.error
    const errors: unknown[][] = [];
    const originalError = console.error;
    console.error = (...args: unknown[]) => void errors.push(args);
    try {
      // @@@SNIPSTART typescript-testing-replay-in-bulk
      const executions = client.workflow.list({
        query: 'TaskQueue="foo"',
      });
      const histories = executions.intoHistories();
      const results = Worker.runReplayHistories(
        {
          workflowsPath: require.resolve('../workflows/foo'),
        },
        histories,
      );
      for await (const result of results) {
        if (result.error) {
          console.error('Replay failed', result);
        }
      }
      // @@@SNIPEND
    } finally {
      console.error = originalError;
    }
    if (errors.length > 0) throw new Error(`Replay failed: ${JSON.stringify(errors)}`);
  });
});

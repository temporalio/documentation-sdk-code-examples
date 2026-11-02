import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { spawn } from 'child_process';
import fs from 'fs';
import path from 'path';
import { bundle } from '../workers/serverless-workers/aws-lambda/scripts/build-workflow-bundle-otel';
import { sampleWorkflow, TASK_QUEUE } from '../workers/serverless-workers/aws-lambda/workflows';
import assert from 'assert';

const dir = path.resolve(__dirname, '../workers/serverless-workers/aws-lambda');

describe('Serverless Worker on AWS Lambda', function () {
  this.timeout(120_000);
  let testEnv: TestWorkflowEnvironment;

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
    await bundle();
  });

  after(async () => {
    await testEnv?.teardown();
    fs.rmSync(path.join(dir, 'workflow-bundle.js'), { force: true });
  });

  it('runs Tasks during one invocation and shuts down before the deadline', async () => {
    // Invoke the handler in its own process, as the Lambda runtime would, so its Runtime and OTel setup are isolated
    const child = spawn(process.execPath, [require.resolve('ts-node/dist/bin'), path.join(dir, 'scripts/invoke-locally.ts')], {
      env: { ...process.env, TEMPORAL_ADDRESS: testEnv.address, INVOCATION_MS: '15000' },
    });
    let output = '';
    child.stdout.on('data', (d) => (output += d));
    child.stderr.on('data', (d) => (output += d));
    const exited = new Promise<number | null>((resolve) => child.once('exit', resolve));
    const started = Date.now();

    for (let i = 0; i < 300 && !output.includes('"state":"RUNNING"'); i++) {
      await new Promise((resolve) => setTimeout(resolve, 100));
    }
    assert.ok(output.includes('"state":"RUNNING"'), output);

    // Serverless Workers always use Worker Versioning, so the version must be current before Workflows run on it
    for (let i = 0; ; i++) {
      try {
        await testEnv.client.workflowService.setWorkerDeploymentCurrentVersion({
          namespace: 'default',
          deploymentName: 'sdk-demo',
          buildId: 'v1',
        });
        break;
      } catch (err) {
        if (i >= 50) throw err;
        await new Promise((resolve) => setTimeout(resolve, 200));
      }
    }

    const result = await testEnv.client.workflow.execute(sampleWorkflow, {
      taskQueue: TASK_QUEUE,
      workflowId: 'lambda-worker',
      args: ['Lambda'],
    });
    assert.equal(result, 'Hello, Lambda!');

    // The Worker shuts down 7 seconds (shutdownDeadlineBufferMs) before the 15-second deadline
    assert.equal(await exited, 0);
    assert.ok(output.includes('"state":"STOPPED"'));
    assert.ok(output.includes('Invocation finished'));
    assert.ok(Date.now() - started < 15_000);
  });
});

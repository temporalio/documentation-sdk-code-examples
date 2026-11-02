import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { bundleWorkflowCode, DefaultLogger, Worker } from '@temporalio/worker';
import { MockActivityEnvironment } from '@temporalio/testing';
import { greet } from '../platform/observability/logging/activities';
import { temporal } from '@temporalio/proto';
import { ChildProcess, spawn } from 'child_process';
import path from 'path';
import { logs } from '../platform/observability/logging/accumulate-logs';
import { listRunning } from '../platform/observability/visibility/list';
import { startWithSearchAttributes } from '../platform/observability/visibility/client';
import { yourWorkflow } from '../platform/observability/visibility/workflows';
import { tracedWorkflow } from '../platform/observability/tracing/workflows';
import assert from 'assert';

const src = path.resolve(__dirname, '..');

// Runs JavaScript in a fresh Node process with ts-node, so each sample can call Runtime.install() itself
function runNode(code: string, env: Record<string, string> = {}): { child: ChildProcess; output: () => string } {
  const child = spawn(process.execPath, ['-r', 'ts-node/register', '-e', code], {
    cwd: path.resolve(src, '..'),
    env: { ...process.env, ...env },
  });
  let output = '';
  child.stdout?.on('data', (d) => (output += d));
  child.stderr?.on('data', (d) => (output += d));
  return { child, output: () => output };
}

const exitOf = (child: ChildProcess) =>
  new Promise<number | null>((resolve) =>
    child.exitCode !== null ? resolve(child.exitCode) : child.once('exit', resolve),
  );

describe('Observability', function () {
  this.timeout(120_000);
  let testEnv: TestWorkflowEnvironment;

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
    const { IndexedValueType } = temporal.api.enums.v1;
    await testEnv.connection.operatorService.addSearchAttributes({
      namespace: 'default',
      searchAttributes: {
        CustomIntField: IndexedValueType.INDEXED_VALUE_TYPE_INT,
        CustomKeywordListField: IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD_LIST,
        CustomBoolField: IndexedValueType.INDEXED_VALUE_TYPE_BOOL,
        CustomDatetimeField: IndexedValueType.INDEXED_VALUE_TYPE_DATETIME,
        CustomTextField: IndexedValueType.INDEXED_VALUE_TYPE_TEXT,
        CustomDoubleField: IndexedValueType.INDEXED_VALUE_TYPE_DOUBLE,
      },
    });
  });

  after(async () => {
    await testEnv?.teardown();
  });

  describe('metrics', () => {
    // Installs the Runtime from a sample, runs a Worker so the SDK emits metrics, and scrapes Prometheus
    async function scrapeMetrics(installCode: string): Promise<string> {
      const { child, output } = runNode(
        `${installCode}
        const { NativeConnection, Worker } = require('@temporalio/worker');
        (async () => {
          const connection = await NativeConnection.connect({ address: process.env.TEMPORAL_ADDRESS });
          const worker = await Worker.create({ connection, taskQueue: 'metrics', activities: { noop: async () => {} } });
          await worker.runUntil(new Promise((resolve) => setTimeout(resolve, 3000)));
          const res = await fetch('http://127.0.0.1:9464/metrics');
          console.log(await res.text());
          await connection.close();
        })().catch((err) => { console.error(err); process.exit(1); });`,
        { TEMPORAL_ADDRESS: testEnv.address },
      );
      assert.equal(await exitOf(child), 0, output());
      return output();
    }

    it('exposes metrics for Prometheus', async () => {
      const metrics = await scrapeMetrics(
        `require('./src/platform/observability/metrics.ts').installPrometheusMetrics();`,
      );
      assert.match(metrics, /^temporal_/m);
    });

    it('attaches global tags to metrics', async () => {
      const metrics = await scrapeMetrics(`require('./src/loggers/telemetry-global-tags.ts');`);
      assert.match(metrics, /team="content-platform"/);
      assert.match(metrics, /cost_center="cc-1042"/);
    });
  });

  describe('logging', () => {
    for (const file of ['loggers/simple-logger.ts', 'platform/observability/logging/default-logger.ts']) {
      it(`routes Runtime logs to a custom logger at WARN and above (${file})`, async () => {
        const { child, output } = runNode(
          `require('./src/${file}');
          const { Runtime } = require('@temporalio/worker');
          Runtime.instance().logger.info('should be filtered out');
          Runtime.instance().logger.warn('should be logged');
          // The Runtime keeps polling for forwarded core logs, so exit once the entries are written
          setTimeout(() => process.exit(0), 500);`,
        );
        assert.equal(await exitOf(child), 0, output());
        assert.match(output(), /Custom logger: WARN — should be logged/);
        assert.doesNotMatch(output(), /should be filtered out/);
      });
    }

    it('sends Runtime logs to winston', async () => {
      // The sample's winston logger has no transports, so winston reports each entry on stderr
      const { child, output } = runNode(
        `require('./src/loggers/custom-logger.ts');
        require('@temporalio/worker').Runtime.instance().logger.info('hello winston', { a: 1 });`,
      );
      assert.equal(await exitOf(child), 0, output());
      assert.match(output(), /"message":"hello winston"/);
      assert.match(output(), /"label":"worker"/);
    });

    it('logs from Workflows with context metadata', async () => {
      const { child, output } = runNode(
        `const { DefaultLogger, NativeConnection, Runtime, Worker } = require('@temporalio/worker');
        const { Client, Connection } = require('@temporalio/client');
        Runtime.install({ logger: new DefaultLogger('INFO', (e) => console.log('LOG', e.message, JSON.stringify(e.meta))) });
        (async () => {
          const nativeConnection = await NativeConnection.connect({ address: process.env.TEMPORAL_ADDRESS });
          const connection = await Connection.connect({ address: process.env.TEMPORAL_ADDRESS });
          const worker = await Worker.create({
            connection: nativeConnection,
            taskQueue: 'logging',
            workflowsPath: require.resolve('./src/platform/observability/logging/workflows.ts'),
          });
          const client = new Client({ connection });
          await worker.runUntil(
            client.workflow.execute('myWorkflow', { taskQueue: 'logging', workflowId: 'logging-wf', args: ['Temporal'] }),
          );
          await connection.close();
          process.exit(0);
        })().catch((err) => { console.error(err); process.exit(1); });`,
        { TEMPORAL_ADDRESS: testEnv.address },
      );
      assert.equal(await exitOf(child), 0, output());
      const line = output().split('\n').find((l) => l.startsWith('LOG Log from workflow'));
      assert.ok(line, output());
      const meta = JSON.parse(line.slice('LOG Log from workflow '.length));
      assert.equal(meta.name, 'Temporal');
      assert.equal(meta.workflowId, 'logging-wf');
      assert.equal(meta.workflowType, 'myWorkflow');
    });

    it('logs from Activities with context metadata', async () => {
      const entries: { message: string; meta?: Record<string, unknown> }[] = [];
      const logger = new DefaultLogger('INFO', (entry) => entries.push(entry));
      const env = new MockActivityEnvironment({ activityType: 'greet' }, { logger });
      assert.equal(await env.run(greet, 'Temporal'), 'Hello, Temporal!');
      const entry = entries.find((e) => e.message === 'Log from activity');
      assert.equal(entry?.meta?.name, 'Temporal');
      assert.equal(entry?.meta?.activityType, 'greet');
    });

    it('accumulates log entries in memory', () => {
      assert.deepEqual(
        logs.map((entry) => [entry.level, entry.message]),
        [
          ['DEBUG', 'hey'],
          ['INFO', 'ho'],
          ['WARN', 'lets'],
          ['ERROR', 'go'],
        ],
      );
    });
  });

  describe('tracing', () => {
    it('bundles and runs a Workflow that sets the Jaeger propagator', async () => {
      const workflowsPath = require.resolve('../platform/observability/tracing/workflows');
      await bundleWorkflowCode({ workflowsPath, logger: { trace() {}, debug() {}, info() {}, warn() {}, error() {}, log() {} } });
      const worker = await Worker.create({ connection: testEnv.nativeConnection, taskQueue: 'tracing', workflowsPath });
      const result = await worker.runUntil(
        testEnv.client.workflow.execute(tracedWorkflow, { taskQueue: 'tracing', workflowId: 'traced' }),
      );
      assert.equal(result, 'traced');
    });
  });

  describe('Visibility', () => {
    let worker: Worker;
    let runPromise: Promise<void>;

    before(async () => {
      worker = await Worker.create({
        connection: testEnv.nativeConnection,
        taskQueue: 'search-attributes',
        workflowsPath: require.resolve('../platform/observability/visibility/workflows'),
      });
      runPromise = worker.run();
    });

    after(async () => {
      worker.shutdown();
      await runPromise;
    });

    it('lists running Workflow Executions', async () => {
      // No Worker polls this Task Queue, so the Workflow stays running
      const handle = await testEnv.client.workflow.start('neverRuns', { taskQueue: 'nobody', workflowId: 'still-running' });
      let ids: string[] = [];
      for (let i = 0; i < 50 && !ids.includes('still-running'); i++) {
        const response = await listRunning({ address: testEnv.address });
        ids = response.executions.map((e) => e.execution?.workflowId ?? '');
        await new Promise((resolve) => setTimeout(resolve, 200));
      }
      assert.ok(ids.includes('still-running'));
      await handle.terminate();
    });

    it('sets Search Attributes at start and upserts them in the Workflow', async () => {
      const atStart = await startWithSearchAttributes(testEnv.client);
      assert.deepEqual(atStart.CustomIntField, [2]);
      assert.deepEqual(atStart.CustomBoolField, [true]);
      const final = await testEnv.client.workflow.getHandle('search-attributes-example-0').result();
      assert.deepEqual(final.CustomIntField, [3]);
      assert.deepEqual(final.CustomDoubleField, [3.14]);
      assert.equal(final.CustomBoolField, undefined);
    });

    it('removes a Search Attribute by setting it to an empty array', async () => {
      const result = await testEnv.client.workflow.execute(yourWorkflow, {
        taskQueue: 'search-attributes',
        workflowId: 'remove-search-attribute',
      });
      assert.equal(result.CustomIntField, undefined);
    });
  });

  describe('sinks', () => {
    it('calls an injected sink from a Workflow', async () => {
      const { child, output } = runNode(`require('./src/platform/observability/sinks/worker.ts');`, {
        TEMPORAL_ADDRESS: testEnv.address,
      });
      try {
        for (let i = 0; i < 300 && !/state: 'RUNNING'/.test(output()); i++) {
          await new Promise((resolve) => setTimeout(resolve, 100));
        }
        const result = await testEnv.client.workflow.execute('sinkWorkflow', { taskQueue: 'sinks', workflowId: 'sinks' });
        assert.equal(result, 'Hello, Temporal!');
        for (let i = 0; i < 50 && !/sending SMS alert!/.test(output()); i++) {
          await new Promise((resolve) => setTimeout(resolve, 100));
        }
        assert.match(output(), /sending SMS alert! \{[\s\S]*workflowId: 'sinks'[\s\S]*message: 'alerter: Workflow Execution started'/);
      } finally {
        child.kill('SIGINT');
        await exitOf(child);
      }
      assert.match(output(), /Worker gracefully shutdown/);
    });
  });
});

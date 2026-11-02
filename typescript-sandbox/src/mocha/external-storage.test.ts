import { TestWorkflowEnvironment } from '@temporalio/testing';
import { after, before, describe, it } from 'mocha';
import { Worker } from '@temporalio/worker';
import { Client } from '@temporalio/client';
import { DataConverter, ExternalStorage } from '@temporalio/common';
import fs from 'fs';
import os from 'os';
import path from 'path';
import assert from 'assert';
import { createDataConverter } from '../best-practices/data-handling/external-storage/data-converter';
import { createAlwaysExternalDataConverter } from '../best-practices/data-handling/external-storage/threshold';
import { createExternalStorage } from '../best-practices/data-handling/external-storage/multiple-drivers';
import { FileSystemStorageDriver } from '../best-practices/data-handling/external-storage/file-system-storage-driver';
import { driver as s3Driver } from '../best-practices/data-handling/external-storage/s3-driver';
import { driver as gcsDriver } from '../best-practices/data-handling/external-storage/gcs-driver';
import { driver as mrapDriver } from '../best-practices/data-handling/external-storage/mrap-driver';
import { generateReport, processDocument } from '../best-practices/data-handling/external-storage/workflows';

const LARGE = 300 * 1024; // Above the 256 KiB threshold

// Lists every file under a directory
function filesUnder(dir: string): string[] {
  if (!fs.existsSync(dir)) return [];
  return fs.readdirSync(dir, { recursive: true, withFileTypes: true })
    .filter((entry) => entry.isFile())
    .map((entry) => path.join(entry.parentPath, entry.name));
}

describe('External Storage', function () {
  this.timeout(60_000);
  let testEnv: TestWorkflowEnvironment;

  before(async () => {
    testEnv = await TestWorkflowEnvironment.createLocal();
  });

  after(async () => {
    await testEnv?.teardown();
  });

  // Runs a Workflow with a Worker and Client that share the same Data Converter
  async function runWith<T>(dataConverter: DataConverter, fn: (client: Client, taskQueue: string) => Promise<T>) {
    const taskQueue = `external-storage-${Math.random().toString(36).slice(2)}`;
    const worker = await Worker.create({
      connection: testEnv.nativeConnection,
      taskQueue,
      workflowsPath: require.resolve('../best-practices/data-handling/external-storage/workflows'),
      dataConverter,
    });
    const client = new Client({ connection: testEnv.connection, dataConverter });
    return worker.runUntil(fn(client, taskQueue));
  }

  describe('custom filesystem driver', () => {
    let rootDir: string;

    before(() => {
      rootDir = fs.mkdtempSync(path.join(os.tmpdir(), 'external-storage-'));
    });

    it('offloads a large argument and keeps only a reference in the Event History', async () => {
      const result = await runWith(createDataConverter(rootDir), (client, taskQueue) =>
        client.workflow.execute(processDocument, {
          args: ['x'.repeat(LARGE)],
          taskQueue,
          workflowId: 'large-document',
        }),
      );
      assert.equal(result, `Processed ${LARGE} characters`);

      // The driver grouped the stored payload under the Workflow that owns it
      const stored = filesUnder(path.join(rootDir, 'default', 'workflow', 'large-document'));
      assert.equal(stored.length, 1);

      const history = await testEnv.client.workflow.getHandle('large-document').fetchHistory();
      const input = history.events?.[0].workflowExecutionStartedEventAttributes?.input?.payloads?.[0];
      assert.ok((input?.data?.length ?? 0) < 1024, 'the Event History holds a small reference, not the document');
    });

    it('offloads a large result and retrieves it on the Client', async () => {
      const report = await runWith(createDataConverter(rootDir), (client, taskQueue) =>
        client.workflow.execute(generateReport, { args: [LARGE], taskQueue, workflowId: 'large-report' }),
      );
      assert.equal(report.length, LARGE);
    });

    it('keeps payloads below the threshold inline', async () => {
      const before = filesUnder(rootDir).length;
      await runWith(createDataConverter(rootDir), (client, taskQueue) =>
        client.workflow.execute(processDocument, { args: ['small'], taskQueue, workflowId: 'small-document' }),
      );
      assert.equal(filesUnder(rootDir).length, before);
    });
  });

  it('offloads every payload with a threshold of 0', async () => {
    const rootDir = fs.mkdtempSync(path.join(os.tmpdir(), 'external-storage-threshold-'));
    await runWith(createAlwaysExternalDataConverter(rootDir), (client, taskQueue) =>
      client.workflow.execute(processDocument, { args: ['small'], taskQueue, workflowId: 'threshold-zero' }),
    );
    assert.ok(filesUnder(rootDir).length > 0);
  });

  it('requires a driverSelector when more than one driver is registered', () => {
    assert.ok(createExternalStorage() instanceof ExternalStorage);
    assert.throws(
      () =>
        new ExternalStorage({
          drivers: [
            new FileSystemStorageDriver({ rootDir: os.tmpdir(), name: 'a' }),
            new FileSystemStorageDriver({ rootDir: os.tmpdir(), name: 'b' }),
          ],
        }),
    );
  });

  it('creates the S3, GCS, and Multi-Region Access Point drivers', () => {
    assert.equal(s3Driver.type, 'aws.s3driver');
    assert.equal(gcsDriver.type, 'gcp.gcsdriver');
    assert.equal(mrapDriver.type, 'aws.s3driver');
  });
});

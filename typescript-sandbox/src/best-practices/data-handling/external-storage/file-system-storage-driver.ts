import { createHash } from 'crypto';
import { promises as fs } from 'fs';
import path from 'path';
import {
  Payload,
  StorageDriver,
  StorageDriverClaim,
  StorageDriverRetrieveContext,
  StorageDriverStoreContext,
} from '@temporalio/common';
import { temporal } from '@temporalio/proto';

export interface FileSystemStorageDriverOptions {
  rootDir: string;
  name?: string;
}

/**
 * Stores each payload as a file on a filesystem that every Client and Worker can reach.
 * Suitable for local development, or for Workers that mount the same volume.
 */
export class FileSystemStorageDriver implements StorageDriver {
  readonly name: string;
  readonly type = 'local.filesystem';
  private readonly rootDir: string;

  constructor({ rootDir, name = 'local.filesystem' }: FileSystemStorageDriverOptions) {
    this.rootDir = rootDir;
    this.name = name;
  }

  async store(context: StorageDriverStoreContext, payloads: Payload[]): Promise<StorageDriverClaim[]> {
    // Group payloads by the Workflow or Activity that owns them
    const owner = context.target
      ? path.join(context.target.namespace, context.target.kind, context.target.id ?? 'unknown')
      : 'unknown';
    return Promise.all(
      payloads.map(async (payload) => {
        // Serialize the whole Payload message, including its metadata
        const bytes = temporal.api.common.v1.Payload.encode(payload).finish();
        // Content-addressable key: identical payloads are stored once
        const hash = createHash('sha256').update(bytes).digest('hex');
        const relativePath = path.join(owner, hash);
        const filePath = path.join(this.rootDir, relativePath);
        await fs.mkdir(path.dirname(filePath), { recursive: true });
        await fs.writeFile(filePath, bytes);
        return new StorageDriverClaim({ path: relativePath, sha256: hash });
      }),
    );
  }

  async retrieve(_context: StorageDriverRetrieveContext, claims: StorageDriverClaim[]): Promise<Payload[]> {
    return Promise.all(
      claims.map(async (claim) => {
        const bytes = await fs.readFile(path.join(this.rootDir, claim.claimData.path));
        const hash = createHash('sha256').update(bytes).digest('hex');
        if (hash !== claim.claimData.sha256) {
          throw new Error(`Stored payload ${claim.claimData.path} doesn't match its hash`);
        }
        return temporal.api.common.v1.Payload.decode(bytes);
      }),
    );
  }
}

# Rust External Storage examples

This independent crate uses an unreleased Rust SDK revision with the `experimental`
feature. It is isolated from the parent crate to keep existing samples on their
published SDK dependencies. All Temporal crates are pinned to the same revision.

The sample sends a 3 MiB string through a Workflow and an echo Activity, checks the
Workflow result, and shuts down its Worker. Temporal connection configuration is
loaded from the standard configuration file and environment variables.

Start a local Temporal Service in another terminal:

```sh
temporal server start-dev
```

Run with local disk (development only; every process must share `payload-store/`):

```sh
cargo run
```

Run with the example S3 driver and AWS credentials configured in your environment.
The crate enables `aws-config/credentials-login` to support `aws login` sessions:

```sh
S3_BUCKET=my-temporal-payloads AWS_REGION=us-east-1 cargo run --features s3
```

The S3 driver needs `s3:PutObject` and `s3:GetObject` on the bucket's
`temporal-payloads/` prefix. Keep objects available for Workflow lifetime and
retention. The sample does not delete objects or configure bucket lifecycle rules.
Both drivers verify SHA-256 hashes and reuse keys for identical serialized payloads.
The local driver does not provide atomic writes or production durability.
Claim metadata is not encrypted by a Payload Codec in this SDK revision.

Validate locally:

```sh
cargo build
cargo test
cargo build --features s3
cargo test --features s3
```

To run the live driver test, use a dedicated, disposable bucket. The test writes,
corrupts, and deletes its own objects:

```sh
RUST_EXTERNAL_STORAGE_TEST_BUCKET=my-disposable-bucket AWS_REGION=us-east-2 \
  cargo test --locked --features s3 s3_live_round_trip_and_failure_paths -- --ignored
```

The live test covers metadata and ordering, retrieval through a fresh driver
instance, repeated writes, checksum failures, missing objects, and malformed claims.
It is ignored by default so ordinary `cargo test` runs need no AWS access.

Verified on November 4, 2026 with AWS login credentials and S3 in `us-east-2`:
local tests, the live driver test, and the complete 3 MiB Workflow/Activity sample
passed. Event History contained four External Storage references of 209 bytes each;
the Service reported a total history size of 2,286 bytes. The temporary test bucket
and objects were removed afterward.

Live S3 access requires your own bucket and credentials. Snipsync source markers
are in `src/`. The companion documentation page is being drafted; its public link
will be added after publication.

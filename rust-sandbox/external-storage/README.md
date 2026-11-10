# Rust External Storage examples

This independent crate uses Rust SDK 1.2.0 with the `experimental` feature and
`temporalio-aws-s3driver` 0.1.0, both published on crates.io. It is isolated from
the parent crate so its optional S3 dependencies stay separate from other samples.
`src/s3_setup.rs` configures the SDK-provided driver; the sample does not implement
its own S3 driver.

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

Run with the SDK-provided S3 driver and AWS credentials configured in your environment.
The crate enables `aws-config/credentials-login` to support `aws login` sessions:

```sh
S3_BUCKET=my-temporal-payloads AWS_REGION=us-east-1 cargo run --features s3
```

The S3 driver needs `s3:PutObject` and `s3:GetObject` for the objects under its
`v0/` prefix, plus `s3:ListBucket` on the bucket. The driver uses `HeadObject` to
skip existing objects; without `s3:ListBucket`, S3 can return 403 for missing
objects instead of 404. Keep objects available for Workflow lifetime and
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

Verified on November 10, 2026 with published Rust SDK 1.2.0 and
`temporalio-aws-s3driver` 0.1.0, using AWS login credentials in `us-east-2`:
the live S3 driver tests and complete 3 MiB Workflow/Activity round trip passed.
Event History contained four reference tokens (386 to 418 bytes each), with a
reported total history size of 3,090 bytes. The temporary S3 objects and bucket
were deleted and cleanup verified.

Build, ordinary tests, the S3 all-targets compile check, formatting, and the
3 MiB local-disk round trip also passed with these published crates.

Live S3 access requires your own bucket and credentials. Snipsync source markers
are in `src/`. The companion documentation page is being drafted; its public link
will be added after publication.

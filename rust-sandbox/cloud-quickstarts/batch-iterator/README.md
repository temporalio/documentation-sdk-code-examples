# Batch Iterator quickstart (Rust)

A minimal, self-contained sample of the **iterator** batch-processing pattern. It
reads its connection settings from Temporal's environment-configuration feature,
so the same Worker and client run against a local dev server or Temporal Cloud
with no code changes.

The parent Workflow loads one page of records, starts one child Workflow per
record, and waits for the whole page. It then **continues as new** with the next
offset, so each run's Event History stays small however big the dataset is. The
sample dataset is generated in memory: 3 pages of 5 records.

## What's here

| File | Purpose |
|---|---|
| `src/workflows.rs` | `IteratorBatchWorkflow` (loads a page, fans out children, continues as new) and `RecordProcessorWorkflow` (processes one record). |
| `src/activities.rs` | `RecordLoader::get_records` Activity that returns the next page of records. |
| `src/lib.rs` | Shared Task Queue name, page size, and the `SingleRecord` type. |
| `src/bin/worker.rs` | Worker that connects via `ClientOptions::load_from_config()` and hosts both Workflows and the Activity. |
| `src/bin/client.rs` | Starts the batch and waits for the result. |
| `tests/batch_iterator.rs` | Runs the whole batch against a local dev server. |
| `config.toml` | Reference profiles (`default` for local, `cloud` for Temporal Cloud). |

`ClientOptions::load_from_config()` (from `temporalio_client::envconfig`) reads:

1. the `TEMPORAL_PROFILE` env var (which profile to load, default `"default"`),
2. the TOML config file (the OS default location, or the path in
   `TEMPORAL_CONFIG_FILE`), and
3. any `TEMPORAL_*` override vars (`TEMPORAL_ADDRESS`, `TEMPORAL_NAMESPACE`,
   `TEMPORAL_API_KEY`, and so on).

The Task Queue defaults to `quickstart-batch-iterator`; set `TEMPORAL_TASK_QUEUE`
to override it for both processes.

## Prerequisites

- A current stable [Rust toolchain](https://rustup.rs/) (edition 2024).
- `protoc` (the Protocol Buffers compiler), which the Temporal SDK needs to build.

Run the commands below from this folder.

## Run against a local dev server

```bash
temporal server start-dev          # terminal 1
cargo run --bin worker             # terminal 2
cargo run --bin client             # terminal 3
```

Expected client output:

```
Started batch Workflow batch-iterator-1794503215276 against http://localhost:7233/ (namespace: default)...
Batch complete: processed 15 records
```

## Run against Temporal Cloud

Fill in the `cloud` profile (see `config.toml`), then select it:

```bash
TEMPORAL_PROFILE=cloud cargo run --bin worker      # terminal 1
TEMPORAL_PROFILE=cloud cargo run --bin client      # terminal 2
```

## Run the test

The test starts its own dev server. Set `TEMPORAL_CLI_PATH` to use an installed
Temporal CLI instead of downloading one:

```bash
TEMPORAL_CLI_PATH=$(which temporal) cargo test
```

## What you'll see in the Temporal UI

- 1 `IteratorBatchWorkflow` with 4 runs: 3 `Continued As New` (one per page)
  and a final `Completed` run that returns `15`.
- 15 `RecordProcessorWorkflow` children, `Completed`, with IDs like
  `batch-iterator-<timestamp>/7`.

# Batch Iterator quickstart (Java)

A minimal, self-contained sample of the **iterator** batch-processing pattern. It
reads its connection settings from Temporal's environment-configuration feature,
so the same Worker and client run against a local dev server or Temporal Cloud
with no code changes.

The parent Workflow loads one page of records, starts one child Workflow per
record, and waits for the whole page. It then **continues as new** with the next
offset, so each run's Event History stays small however big the dataset is. The
sample dataset is generated in memory: 3 pages of 5 records.

Ported from
[`samples-java` `batch/iterator`](https://github.com/temporalio/samples-java/tree/main/core/src/main/java/io/temporal/samples/batch/iterator),
with the dataset and per-record delay shortened so a run finishes in seconds.

## What's here

| File | Purpose |
|---|---|
| `IteratorBatchWorkflow.java` / `IteratorBatchWorkflowImpl.java` | Parent Workflow: loads a page, fans out children, continues as new. |
| `RecordProcessorWorkflow.java` / `RecordProcessorWorkflowImpl.java` | Child Workflow that processes one record. |
| `RecordLoader.java` / `RecordLoaderImpl.java` | Activity that returns the next page of records. |
| `SingleRecord.java` | A record. |
| `BatchIterator.java` | Shared Task Queue name and page size. |
| `Worker.java` | Worker that connects via `ClientConfigProfile.load()` and hosts both Workflows and the Activity. |
| `Client.java` | Starts the batch and waits for the result. |
| `config.toml` | Reference profiles (`default` for local, `cloud` for Temporal Cloud). |

`ClientConfigProfile.load()` (from `io.temporal:temporal-envconfig`) reads:

1. the `TEMPORAL_PROFILE` env var (which profile to load, default `"default"`),
2. the TOML config file (the OS default location, or the path in
   `TEMPORAL_CONFIG_FILE`), and
3. any `TEMPORAL_*` override vars (`TEMPORAL_ADDRESS`, `TEMPORAL_NAMESPACE`,
   `TEMPORAL_API_KEY`, and so on).

The Task Queue defaults to `quickstart-batch-iterator`; set `TEMPORAL_TASK_QUEUE`
to override it for both processes.

## Prerequisites

- JDK 21 or later.
- [Gradle](https://gradle.org/install/). Commands below use `gradle`.

## Run against a local dev server

```bash
temporal server start-dev          # terminal 1
gradle -q runWorker                # terminal 2
gradle -q runClient                # terminal 3
```

Expected client output:

```
Started batch Workflow batch-iterator-1793304967710 against "localhost:7233" (namespace "default")...
Batch complete: processed 15 records
```

## Run against Temporal Cloud

Fill in the `cloud` profile (see `config.toml`), then select it:

```bash
TEMPORAL_PROFILE=cloud gradle -q runWorker
TEMPORAL_PROFILE=cloud gradle -q runClient
```

## What you'll see in the Temporal UI

- 1 `IteratorBatchWorkflow` with 4 runs: 3 `Continued As New` (one per page)
  and a final `Completed` run that returns `15`.
- 15 `RecordProcessorWorkflow` children, `Completed`, with IDs like
  `batch-iterator-<timestamp>/7`.

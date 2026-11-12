# Batch Iterator quickstart (.NET)

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
| `IteratorBatchWorkflow.workflow.cs` | Parent Workflow: loads a page, fans out children, continues as new. |
| `RecordProcessorWorkflow.workflow.cs` | Child Workflow that processes one record. |
| `RecordLoader.cs` | `GetRecords` Activity that returns the next page of records. |
| `Shared.cs` | Shared Task Queue name, page size, and the `SingleRecord` record. |
| `Program.cs` | `worker` runs the Worker; `client` starts the batch and waits for the result. Both connect via `ClientEnvConfig.LoadClientConnectOptions()`. |
| `config.toml` | Reference profiles (`default` for local, `cloud` for Temporal Cloud). |
| `../BatchIterator.Tests/` | Runs the whole batch in a time-skipping test environment. |

`ClientEnvConfig.LoadClientConnectOptions()` (from `Temporalio.Common.EnvConfig`) reads:

1. the `TEMPORAL_PROFILE` env var (which profile to load, default `"default"`),
2. the TOML config file (the OS default location, or the path in
   `TEMPORAL_CONFIG_FILE`), and
3. any `TEMPORAL_*` override vars (`TEMPORAL_ADDRESS`, `TEMPORAL_NAMESPACE`,
   `TEMPORAL_API_KEY`, and so on).

The Task Queue defaults to `quickstart-batch-iterator`; set `TEMPORAL_TASK_QUEUE`
to override it for both processes.

## Prerequisites

- [.NET SDK](https://dotnet.microsoft.com/download) 10.0 or later.

Run the commands below from this folder.

## Run against a local dev server

```bash
temporal server start-dev          # terminal 1
dotnet run -- worker               # terminal 2
dotnet run -- client               # terminal 3
```

Expected client output:

```
Started batch Workflow batch-iterator-1794498782358 against localhost:7233 (namespace: default)...
Batch complete: processed 15 records
```

## Run against Temporal Cloud

Fill in the `cloud` profile (see `config.toml`), then select it:

```bash
TEMPORAL_PROFILE=cloud dotnet run -- worker      # terminal 1
TEMPORAL_PROFILE=cloud dotnet run -- client      # terminal 2
```

## Run the test

```bash
dotnet test ../BatchIterator.Tests
```

## What you'll see in the Temporal UI

- 1 `IteratorBatchWorkflow` with 4 runs: 3 `Continued As New` (one per page)
  and a final `Completed` run that returns `15`.
- 15 `RecordProcessorWorkflow` children, `Completed`, with IDs like
  `batch-iterator-<timestamp>/7`.

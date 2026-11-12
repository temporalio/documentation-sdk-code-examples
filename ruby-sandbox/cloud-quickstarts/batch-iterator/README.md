# Batch Iterator quickstart (Ruby)

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
| `iterator_batch_workflow.rb` | Parent Workflow: loads a page, fans out children, continues as new. |
| `record_processor_workflow.rb` | Child Workflow that processes one record. |
| `get_records_activity.rb` | Activity that returns the next page of records (each a Hash like `{ 'id' => 7 }`). |
| `shared.rb` | Shared Task Queue name and page size. |
| `connection.rb` | Loads connection settings via `Temporalio::EnvConfig::ClientConfig.load_client_connect_options`. |
| `worker.rb` | Worker that hosts both Workflows and the Activity. |
| `client.rb` | Starts the batch and waits for the result. |
| `test/batch_iterator_test.rb` | Runs the whole batch in a time-skipping test environment. |
| `config.toml` | Reference profiles (`default` for local, `cloud` for Temporal Cloud). |

`load_client_connect_options` (from `temporalio/env_config`) reads:

1. the `TEMPORAL_PROFILE` env var (which profile to load, default `"default"`),
2. the TOML config file (the OS default location, or the path in
   `TEMPORAL_CONFIG_FILE`), and
3. any `TEMPORAL_*` override vars (`TEMPORAL_ADDRESS`, `TEMPORAL_NAMESPACE`,
   `TEMPORAL_API_KEY`, and so on).

The Task Queue defaults to `quickstart-batch-iterator`; set `TEMPORAL_TASK_QUEUE`
to override it for both processes.

## Prerequisites

- Ruby 3.3 or later.
- Bundler. Run `bundle install` once from `ruby-sandbox/`; this sample uses that
  folder's `Gemfile`.

Run the commands below from this folder.

## Run against a local dev server

```bash
temporal server start-dev              # terminal 1
bundle exec ruby worker.rb             # terminal 2
bundle exec ruby client.rb             # terminal 3
```

Expected client output:

```
Started batch Workflow batch-iterator-1794499163875 against localhost:7233 (namespace: default)...
Batch complete: processed 15 records
```

## Run against Temporal Cloud

Fill in the `cloud` profile (see `config.toml`), then select it:

```bash
TEMPORAL_PROFILE=cloud bundle exec ruby worker.rb      # terminal 1
TEMPORAL_PROFILE=cloud bundle exec ruby client.rb      # terminal 2
```

## Run the test

```bash
bundle exec ruby test/batch_iterator_test.rb
```

## What you'll see in the Temporal UI

- 1 `IteratorBatchWorkflow` with 4 runs: 3 `Continued As New` (one per page)
  and a final `Completed` run that returns `15`.
- 15 `RecordProcessorWorkflow` children, `Completed`, with IDs like
  `batch-iterator-<timestamp>/7`.

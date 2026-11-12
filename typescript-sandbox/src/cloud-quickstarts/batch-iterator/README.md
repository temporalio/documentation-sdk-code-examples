# Batch Iterator quickstart (TypeScript)

A minimal, self-contained sample of the **iterator** batch-processing pattern. It
reads its connection settings from Temporal's environment-configuration feature,
so the same Worker and client run against a local dev server or Temporal Cloud
with no code changes.

The parent Workflow loads one page of records, starts one child Workflow per
record, and waits for the whole page. It then **continues as new** with the next
offset, so each run's Event History stays small however big the dataset is. The
sample dataset is generated in memory: 3 pages of 5 records.

## What's here

| File                                 | Purpose                                                                                                                           |
| ------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------- |
| `workflows.ts`                       | `iteratorBatchWorkflow` (loads a page, fans out children, continues as new) and `recordProcessorWorkflow` (processes one record). |
| `activities.ts`                      | `getRecords` Activity that returns the next page of records.                                                                      |
| `types.ts`                           | The `SingleRecord` type.                                                                                                          |
| `shared.ts`                          | Shared Task Queue name and page size.                                                                                             |
| `worker.ts`                          | Worker that connects via `loadClientConnectConfig()` and hosts both Workflows and the Activity.                                   |
| `client.ts`                          | Starts the batch and waits for the result.                                                                                        |
| `config.toml`                        | Reference profiles (`default` for local, `cloud` for Temporal Cloud).                                                             |
| `../../mocha/batch-iterator.test.ts` | Runs the whole batch in a time-skipping test environment.                                                                         |

`loadClientConnectConfig()` (from `@temporalio/envconfig`) reads:

1. the `TEMPORAL_PROFILE` env var (which profile to load, default `"default"`),
2. the TOML config file (the OS default location, or the path in
   `TEMPORAL_CONFIG_FILE`), and
3. any `TEMPORAL_*` override vars (`TEMPORAL_ADDRESS`, `TEMPORAL_NAMESPACE`,
   `TEMPORAL_API_KEY`, and so on).

The Task Queue defaults to `quickstart-batch-iterator`; set `TEMPORAL_TASK_QUEUE`
to override it for both processes.

## Prerequisites

- Node.js (the version in `typescript-sandbox/.nvmrc`).
- Run `npm install` once from `typescript-sandbox/`; this sample uses that
  folder's `package.json`.

Run the commands below from `typescript-sandbox/`.

## Run against a local dev server

```bash
temporal server start-dev              # terminal 1
npm run batch-iterator.worker          # terminal 2
npm run batch-iterator.client          # terminal 3
```

Expected client output:

```
Started batch Workflow batch-iterator-1794513795035 against localhost:7233 (namespace: default)...
Batch complete: processed 15 records
```

## Run against Temporal Cloud

Fill in the `cloud` profile (see `config.toml`), then select it:

```bash
TEMPORAL_PROFILE=cloud npm run batch-iterator.worker      # terminal 1
TEMPORAL_PROFILE=cloud npm run batch-iterator.client      # terminal 2
```

## Run the test

```bash
npx mocha --exit --node-option no-experimental-strip-types --require ts-node/register \
  src/mocha/batch-iterator.test.ts
```

`npm test` runs it along with every other test in the sandbox.

## What you'll see in the Temporal UI

- 1 `iteratorBatchWorkflow` with 4 runs: 3 `Continued As New` (one per page)
  and a final `Completed` run that returns `15`.
- 15 `recordProcessorWorkflow` children, `Completed`, with IDs like
  `batch-iterator-<timestamp>/7`.

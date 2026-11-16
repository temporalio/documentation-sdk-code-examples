# Human-in-the-loop quickstart (Rust)

A minimal, self-contained sample of an AI agent that asks a human to approve
risky actions. It reads its connection settings from Temporal's
environment-configuration feature, so the same Worker and clients run against a
local dev server or Temporal Cloud with no code changes.

The Workflow asks an OpenAI model to propose an action for the user's request.
Safe actions run right away. For a risky action, the Workflow notifies a
reviewer and then waits for an approval decision, which arrives as a
**Signal**. The wait is a durable timer: the Workflow uses no compute while it
waits, and if nobody responds before the timeout (5 minutes by default), the
Workflow cancels the action.

## What's here

| File | Purpose |
|---|---|
| `src/workflows.rs` | `HumanInTheLoopWorkflow`, its `approval_decision` Signal handler, and the agent's system instructions. |
| `src/activities.rs` | `propose_action` (asks the OpenAI Responses API for a structured `ProposedAction`, or returns a mock action when `OPENAI_API_KEY` isn't set), `notify_approval_needed`, and `execute_action` Activities. |
| `src/lib.rs` | Shared Task Queue name, Workflow ID, and the types passed between the Workflow, Activities, and clients. |
| `src/bin/worker.rs` | Worker that connects via `ClientOptions::load_from_config` and hosts the Workflow and Activities. |
| `src/bin/client.rs` | Starts the Workflow for a request and waits for the result. |
| `src/bin/send_approval.rs` | Sends an approve or reject decision to the waiting Workflow as a Signal. |
| `tests/human_in_the_loop.rs` | Runs the auto-approve, approve, reject, and timeout paths against a local dev server, with the OpenAI call mocked, plus a check of the no-API-key fallback. |
| `config.toml` | Reference profiles (`default` for local, `cloud` for Temporal Cloud). |

`ClientOptions::load_from_config` (from `temporalio-client`) reads:

1. the `TEMPORAL_PROFILE` env var (which profile to load, default `"default"`),
2. the TOML config file (the OS default location, or the path in
   `TEMPORAL_CONFIG_FILE`), and
3. any `TEMPORAL_*` override vars (`TEMPORAL_ADDRESS`, `TEMPORAL_NAMESPACE`,
   `TEMPORAL_API_KEY`, and so on).

The Task Queue defaults to `quickstart-human-in-the-loop`; set
`TEMPORAL_TASK_QUEUE` to override it for every process.

## Prerequisites

- A current stable [Rust toolchain](https://rustup.rs/) (edition 2024).
- `protoc` (the Protocol Buffers compiler), which the Temporal SDK needs to build.
- Optional: an OpenAI API key, exported in the Worker's terminal:

  ```bash
  export OPENAI_API_KEY='your-api-key-here'
  ```

  Without a key, the `propose_action` Activity skips OpenAI and returns a
  fixed, risky `delete_test_data` action, so you can still run the full
  approval flow. The Worker prints a warning when it uses the mock.

  The Activity calls the Responses API with `reqwest`, using rustls with the
  `ring` crypto provider that the Temporal SDK already depends on.

Run the commands below from this folder.

## Run against a local dev server

```bash
temporal server start-dev          # terminal 1
cargo run --bin worker             # terminal 2
cargo run --bin client             # terminal 3
```

The client uses a risky default request ("Delete all test data from the
production database"). Pass your own request after `--`:

```bash
cargo run --bin client -- "List the files in the reports folder"
```

The Workflow always uses the ID `human-in-the-loop` (set
`HUMAN_IN_THE_LOOP_WORKFLOW_ID` to change it), so only one can run at a time.
When the model marks the action as risky, the Worker prints an
`APPROVAL REQUIRED` block. Approve or reject in a fourth terminal:

```bash
cargo run --bin send_approval -- approve "Looks good"
cargo run --bin send_approval -- reject "Too risky"
```

The decision is a Signal named `approval_decision`, so you can also send it
with the Temporal CLI, or with **Send a Signal** on the Workflow's page in the
Temporal UI, using the same JSON input:

```bash
temporal workflow signal --workflow-id human-in-the-loop \
  --name approval_decision \
  --input '{"approved": true, "reviewer_notes": "Looks good"}'
```

The Signal applies to whichever approval request is pending. To target one
request, add its `request_id` (printed by the Worker) to the JSON input. The
Workflow then ignores a decision whose `request_id` doesn't match. It also
ignores a decision sent before any request is pending.

Expected client output after you approve:

```
Started Workflow human-in-the-loop against http://localhost:7233/ (namespace: default)...
Request: Delete all test data from the production database
If the action is risky, the Worker prints how to approve or reject it.
Result: Action completed successfully: Successfully executed: delete_test_data
```

With an API key, the `action_type` in the result comes from the model, so it
can vary between runs. Without one, the mock always proposes `delete_test_data`
and always asks for approval, even for a safe request. If you send no decision,
the Workflow returns
`Action cancelled: approval request timed out after 300 seconds`.

If you start the client while an earlier run is still waiting, it exits and
tells you to approve, reject, or terminate that run first.

## Run against Temporal Cloud

Fill in the `cloud` profile (see `config.toml`), then select it:

```bash
TEMPORAL_PROFILE=cloud cargo run --bin worker                      # terminal 1
TEMPORAL_PROFILE=cloud cargo run --bin client                      # terminal 2
TEMPORAL_PROFILE=cloud cargo run --bin send_approval -- approve    # terminal 3
```

## Run the tests

The tests replace the OpenAI Activity with a mock, so they need no API key. The
no-API-key fallback test is skipped when `OPENAI_API_KEY` is set. The tests
start their own dev server; set `TEMPORAL_CLI_PATH` to use an installed
Temporal CLI instead of downloading one:

```bash
TEMPORAL_CLI_PATH=$(which temporal) cargo test
```

## What you'll see in the Temporal UI

- A `HumanInTheLoopWorkflow` that stays `Running` while it waits for a
  decision.
- In its Event History: the `propose_action` and `notify_approval_needed`
  Activities, a `TimerStarted` event for the approval timeout, a
  `WorkflowExecutionSignaled` event when you send a decision, and then the
  `execute_action` Activity if you approved.

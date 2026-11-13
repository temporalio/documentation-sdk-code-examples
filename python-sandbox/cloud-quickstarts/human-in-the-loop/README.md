# Human-in-the-loop quickstart (Python)

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
| `workflow.py` | `HumanInTheLoopWorkflow`, its `approval_decision` Signal handler, and the agent's system instructions. |
| `activities.py` | `create` (calls the OpenAI Responses API), `notify_approval_needed`, and `execute_action` Activities. |
| `shared.py` | Shared Task Queue name and the Pydantic models passed between the Workflow, Activities, and clients. |
| `worker.py` | Worker that connects via `ClientConfig.load_client_connect_config()` and hosts the Workflow and Activities. |
| `client.py` | Starts the Workflow for a request and waits for the result. |
| `send_approval.py` | Sends an approve or reject decision to a waiting Workflow as a Signal. |
| `test_human_in_the_loop.py` | Runs the auto-approve, approve, reject, and timeout paths in a time-skipping test environment, with the OpenAI call mocked. |
| `config.toml` | Reference profiles (`default` for local, `cloud` for Temporal Cloud). |

`load_client_connect_config()` (from `temporalio.envconfig`) reads:

1. the `TEMPORAL_PROFILE` env var (which profile to load, default `"default"`),
2. the TOML config file (the OS default location, or the path in
   `TEMPORAL_CONFIG_FILE`), and
3. any `TEMPORAL_*` override vars (`TEMPORAL_ADDRESS`, `TEMPORAL_NAMESPACE`,
   `TEMPORAL_API_KEY`, and so on).

The Task Queue defaults to `quickstart-human-in-the-loop`; set
`TEMPORAL_TASK_QUEUE` to override it for every process.

## Prerequisites

- Python 3.10 or later.
- [uv](https://docs.astral.sh/uv/) (`pip install uv`). `uv sync` installs the
  dependencies pinned in `uv.lock`.
- An OpenAI API key, exported in the Worker's terminal:

  ```bash
  export OPENAI_API_KEY='your-api-key-here'
  ```

## Run against a local dev server

```bash
temporal server start-dev          # terminal 1
uv run worker.py                   # terminal 2
uv run client.py                   # terminal 3
```

`client.py` uses a risky default request ("Delete all test data from the
production database"). Pass your own request as an argument:

```bash
uv run client.py "List the files in the reports folder"
```

When the model marks the action as risky, the Worker prints an
`APPROVAL REQUIRED` block with the Workflow ID, the request ID, and the commands
to approve or reject. Run one of them in a fourth terminal:

```bash
uv run send_approval.py <workflow-id> <request-id> approve "Looks good"
uv run send_approval.py <workflow-id> <request-id> reject "Too risky"
```

Expected client output after you approve:

```
Started Workflow human-in-the-loop-1794497439014 against localhost:7233 (namespace: default)...
Request: Delete all test data from the production database
If the action is risky, the Worker prints how to approve or reject it.
Result: Action completed successfully: Successfully executed: delete_test_data
```

The `action_type` in the result comes from the model, so it can vary between
runs. If you send no decision, the Workflow returns
`Action cancelled: approval request timed out after 300 seconds`.

## Run against Temporal Cloud

Fill in the `cloud` profile (see `config.toml`), then select it:

```bash
TEMPORAL_PROFILE=cloud uv run worker.py                  # terminal 1
TEMPORAL_PROFILE=cloud uv run client.py                  # terminal 2
TEMPORAL_PROFILE=cloud uv run send_approval.py ...       # terminal 3
```

## Run the test

The test replaces the OpenAI Activity with a mock, so it needs no API key.

```bash
uv run pytest
```

## What you'll see in the Temporal UI

- A `HumanInTheLoopWorkflow` that stays `Running` while it waits for a
  decision.
- In its Event History: the `create` and `notify_approval_needed` Activities, a
  `TimerStarted` event for the approval timeout, a
  `WorkflowExecutionSignaled` event when you send a decision, and then the
  `execute_action` Activity if you approved.

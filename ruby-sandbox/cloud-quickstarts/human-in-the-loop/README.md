# Human-in-the-loop quickstart (Ruby)

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
| `human_in_the_loop_workflow.rb` | `HumanInTheLoopWorkflow`, its `approval_decision` Signal handler, and the agent's system instructions. |
| `activities.rb` | `ProposeActionActivity` (asks the OpenAI Responses API for a structured proposed action, or returns a mock action when `OPENAI_API_KEY` isn't set), `NotifyApprovalNeededActivity`, and `ExecuteActionActivity`. |
| `shared.rb` | Shared Task Queue name and Workflow ID, plus the shape of the Hashes passed between the Workflow, Activities, and clients. |
| `connection.rb` | Loads connection settings via `Temporalio::EnvConfig::ClientConfig.load_client_connect_options`. |
| `worker.rb` | Worker that hosts the Workflow and Activities. |
| `client.rb` | Starts the Workflow for a request and waits for the result. |
| `send_approval.rb` | Sends an approve or reject decision to the waiting Workflow as a Signal. |
| `test/human_in_the_loop_test.rb` | Runs the auto-approve, approve, reject, and timeout paths in a time-skipping test environment, with the OpenAI call mocked, plus a check of the no-API-key fallback. |
| `config.toml` | Reference profiles (`default` for local, `cloud` for Temporal Cloud). |

`load_client_connect_options` (from `temporalio/env_config`) reads:

1. the `TEMPORAL_PROFILE` env var (which profile to load, default `"default"`),
2. the TOML config file (the OS default location, or the path in
   `TEMPORAL_CONFIG_FILE`), and
3. any `TEMPORAL_*` override vars (`TEMPORAL_ADDRESS`, `TEMPORAL_NAMESPACE`,
   `TEMPORAL_API_KEY`, and so on).

The Task Queue defaults to `quickstart-human-in-the-loop`; set
`TEMPORAL_TASK_QUEUE` to override it for every process.

## Prerequisites

- Ruby 3.3 or later.
- Bundler. Run `bundle install` once from `ruby-sandbox/`; this sample uses that
  folder's `Gemfile`.
- Optional: an OpenAI API key, exported in the Worker's terminal:

  ```bash
  export OPENAI_API_KEY='your-api-key-here'
  ```

  Without a key, `ProposeActionActivity` skips OpenAI and returns a fixed,
  risky `delete_test_data` action, so you can still run the full approval
  flow. The Worker logs a warning when it uses the mock.

  The Activity calls the Responses API with Ruby's built-in `Net::HTTP`, so the
  sample needs no extra gems.

Run the commands below from this folder.

## Run against a local dev server

```bash
temporal server start-dev              # terminal 1
bundle exec ruby worker.rb             # terminal 2
bundle exec ruby client.rb             # terminal 3
```

`client.rb` uses a risky default request ("Delete all test data from the
production database"). Pass your own request as an argument:

```bash
bundle exec ruby client.rb "List the files in the reports folder"
```

The Workflow always uses the ID `human-in-the-loop` (set
`HUMAN_IN_THE_LOOP_WORKFLOW_ID` to change it), so only one can run at a time.
When the model marks the action as risky, the Worker prints an
`APPROVAL REQUIRED` block. Approve or reject in a fourth terminal:

```bash
bundle exec ruby send_approval.rb approve 'Looks good'
bundle exec ruby send_approval.rb reject 'Too risky'
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
Started Workflow human-in-the-loop against localhost:7233 (namespace: default)...
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
TEMPORAL_PROFILE=cloud bundle exec ruby worker.rb                  # terminal 1
TEMPORAL_PROFILE=cloud bundle exec ruby client.rb                  # terminal 2
TEMPORAL_PROFILE=cloud bundle exec ruby send_approval.rb approve   # terminal 3
```

## Run the test

The test replaces the OpenAI Activity with a mock, so it needs no API key.

```bash
bundle exec ruby test/human_in_the_loop_test.rb
```

## What you'll see in the Temporal UI

- A `HumanInTheLoopWorkflow` that stays `Running` while it waits for a
  decision.
- In its Event History: the `ProposeActionActivity` and
  `NotifyApprovalNeededActivity` Activities, a `TimerStarted` event for the
  approval timeout, a `WorkflowExecutionSignaled` event when you send a
  decision, and then the `ExecuteActionActivity` if you approved.

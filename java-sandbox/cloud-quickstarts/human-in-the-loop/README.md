# Human-in-the-loop quickstart (Java)

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

All source files are in `src/main/java/quickstart/`.

| File | Purpose |
|---|---|
| `HumanInTheLoopWorkflow.java` / `HumanInTheLoopWorkflowImpl.java` | The Workflow, its `approval_decision` Signal handler, and the agent's system instructions. |
| `AgentActivities.java` / `AgentActivitiesImpl.java` | `proposeAction` (asks the OpenAI Responses API for a structured `ProposedAction`, or returns a mock action when `OPENAI_API_KEY` isn't set), `notifyApprovalNeeded`, and `executeAction` Activities. |
| `Models.java` | The records passed between the Workflow, Activities, and clients. |
| `HumanInTheLoop.java` | Shared Task Queue name, Workflow ID, and Signal name. |
| `Worker.java` | Worker that connects via `ClientConfigProfile.load()` and hosts the Workflow and Activities. |
| `Client.java` | Starts the Workflow for a request and waits for the result. |
| `SendApproval.java` | Sends an approve or reject decision to the waiting Workflow as a Signal. |
| `src/test/java/quickstart/HumanInTheLoopWorkflowTest.java` | Runs the auto-approve, approve, reject, and timeout paths in the time-skipping test environment, with the OpenAI call mocked, plus a check of the no-API-key fallback. |
| `config.toml` | Reference profiles (`default` for local, `cloud` for Temporal Cloud). |

`ClientConfigProfile.load()` (from `io.temporal:temporal-envconfig`) reads:

1. the `TEMPORAL_PROFILE` env var (which profile to load, default `"default"`),
2. the TOML config file (the OS default location, or the path in
   `TEMPORAL_CONFIG_FILE`), and
3. any `TEMPORAL_*` override vars (`TEMPORAL_ADDRESS`, `TEMPORAL_NAMESPACE`,
   `TEMPORAL_API_KEY`, and so on).

The Task Queue defaults to `quickstart-human-in-the-loop`; set
`TEMPORAL_TASK_QUEUE` to override it for every process.

## Prerequisites

- JDK 21 or later.
- [Maven](https://maven.apache.org/install.html).
- Optional: an OpenAI API key, exported in the Worker's terminal:

  ```bash
  export OPENAI_API_KEY='your-api-key-here'
  ```

  Without a key, the `proposeAction` Activity skips OpenAI and returns a fixed,
  risky `delete_test_data` action, so you can still run the full approval
  flow. The Worker logs a warning when it uses the mock.

Run the commands below from this folder. Each `exec:java@<id>` runner is defined
in `pom.xml`.

## Run against a local dev server

```bash
temporal server start-dev                  # terminal 1
mvn -q compile exec:java@worker            # terminal 2
mvn -q compile exec:java@client            # terminal 3
```

`client` uses a risky default request ("Delete all test data from the
production database"). Pass your own request in `exec.args`:

```bash
mvn -q compile exec:java@client -Dexec.args="'List the files in the reports folder'"
```

The Workflow always uses the ID `human-in-the-loop` (set
`HUMAN_IN_THE_LOOP_WORKFLOW_ID` to change it), so only one can run at a time.
When the model marks the action as risky, the Worker prints an
`APPROVAL REQUIRED` block. Approve or reject in a fourth terminal:

```bash
mvn -q compile exec:java@send-approval -Dexec.args="approve 'Looks good'"
mvn -q compile exec:java@send-approval -Dexec.args="reject 'Too risky'"
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
TEMPORAL_PROFILE=cloud mvn -q compile exec:java@worker                                    # terminal 1
TEMPORAL_PROFILE=cloud mvn -q compile exec:java@client                                    # terminal 2
TEMPORAL_PROFILE=cloud mvn -q compile exec:java@send-approval -Dexec.args="approve"       # terminal 3
```

## Run the tests

The tests replace the OpenAI Activity with a mock, so they need no API key. The
no-API-key fallback test is skipped when `OPENAI_API_KEY` is set.

```bash
mvn verify
```

## What you'll see in the Temporal UI

- A `HumanInTheLoopWorkflow` that stays `Running` while it waits for a
  decision.
- In its Event History: the `ProposeAction` and `NotifyApprovalNeeded`
  Activities, a `TimerStarted` event for the approval timeout, a
  `WorkflowExecutionSignaled` event when you send a decision, and then the
  `ExecuteAction` Activity if you approved.

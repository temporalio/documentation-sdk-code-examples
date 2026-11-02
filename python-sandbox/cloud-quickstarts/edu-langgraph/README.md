# LangGraph quickstart (Python)

A minimal, self-contained sample that runs a [LangGraph](https://langchain-ai.github.io/langgraph/)
graph on Temporal with the `LangGraphPlugin`. It reads its connection settings
from Temporal's environment-configuration feature, so the same Worker and
client run against a local dev server or Temporal Cloud with no code changes.

The graph has a single node, `process_query`, which the plugin runs as a
Temporal Activity. The node is a pure string transform, so the sample needs no
model provider or API key, and the result is always the same.

Ported from
[`samples-python` `langgraph_plugin/graph_api/hello_world`](https://github.com/temporalio/samples-python/tree/main/langgraph_plugin/graph_api/hello_world).

## What's here

| File | Purpose |
|---|---|
| `workflow.py` | The `process_query` node, the graph definition, and `HelloWorldWorkflow`. |
| `worker.py` | Worker that connects via `ClientConfig.load_client_connect_config()` and registers the graph with `LangGraphPlugin`. |
| `client.py` | Runs `HelloWorldWorkflow` and prints the result. |
| `shared.py` | Shared Task Queue name. |
| `config.toml` | Reference profiles (`default` for local, `cloud` for Temporal Cloud). |

`load_client_connect_config()` (from `temporalio.envconfig`) reads:

1. the `TEMPORAL_PROFILE` env var (which profile to load, default `"default"`),
2. the TOML config file (the OS default location, or the path in
   `TEMPORAL_CONFIG_FILE`), and
3. any `TEMPORAL_*` override vars (`TEMPORAL_ADDRESS`, `TEMPORAL_NAMESPACE`,
   `TEMPORAL_API_KEY`, and so on).

The Task Queue defaults to `quickstart-langgraph`; set `TEMPORAL_TASK_QUEUE` to
override it for both processes.

## Prerequisites

- Python 3.10 or later.
- [uv](https://docs.astral.sh/uv/) (`pip install uv`). `uv sync` installs the
  dependencies pinned in `uv.lock`.

## Run against a local dev server

```bash
temporal server start-dev          # terminal 1
uv run worker.py                   # terminal 2
uv run client.py                   # terminal 3
```

Expected client output:

```
Running LangGraph Workflow against localhost:7233 (namespace: default)...
Result: Processed: Hello, Temporal + LangGraph!
```

## Run against Temporal Cloud

Fill in the `cloud` profile (see `config.toml`), then select it:

```bash
TEMPORAL_PROFILE=cloud uv run worker.py      # terminal 1
TEMPORAL_PROFILE=cloud uv run client.py      # terminal 2
```

import asyncio

from temporalio.client import Client
from temporalio.envconfig import ClientConfig
from temporalio.langgraph import LangGraphPlugin
from temporalio.worker import Worker

from shared import TASK_QUEUE
from workflow import GRAPH_NAME, HelloWorldWorkflow, make_hello_graph


async def main() -> None:
    # load_client_connect_config reads the TEMPORAL_PROFILE env var (default
    # "default"), loads that profile from the TOML config file (default OS config
    # dir, or TEMPORAL_CONFIG_FILE), and applies any TEMPORAL_* overrides. No
    # connection details are hardcoded, so the same Worker runs against a local
    # dev server or Temporal Cloud.
    connect_config = ClientConfig.load_client_connect_config()
    # With no config file and no env overrides the loaded config is empty, so
    # fall back to the local dev server address.
    connect_config.setdefault("target_host", "localhost:7233")

    client = await Client.connect(**connect_config)

    # The plugin runs each graph node marked execute_in="activity" as a Temporal
    # Activity on this Worker.
    plugin = LangGraphPlugin(graphs={GRAPH_NAME: make_hello_graph()})
    worker = Worker(
        client,
        task_queue=TASK_QUEUE,
        workflows=[HelloWorldWorkflow],
        plugins=[plugin],
    )

    address = connect_config.get("target_host")
    namespace = connect_config.get("namespace") or "default"
    print(
        f"Worker connected to {address} (namespace: {namespace}), "
        f"polling task queue '{TASK_QUEUE}'",
        flush=True,
    )

    await worker.run()


if __name__ == "__main__":
    asyncio.run(main())

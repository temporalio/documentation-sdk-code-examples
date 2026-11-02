import asyncio
import time

from temporalio.client import Client
from temporalio.envconfig import ClientConfig

from shared import TASK_QUEUE
from workflow import HelloWorldWorkflow


async def main() -> None:
    # Same environment-configuration lookup the Worker uses: TEMPORAL_PROFILE
    # selects the profile, the TOML file supplies address/namespace/credentials,
    # and TEMPORAL_* env vars can override individual fields.
    connect_config = ClientConfig.load_client_connect_config()
    connect_config.setdefault("target_host", "localhost:7233")

    client = await Client.connect(**connect_config)

    address = connect_config.get("target_host")
    namespace = connect_config.get("namespace") or "default"
    print(f"Running LangGraph Workflow against {address} (namespace: {namespace})...")

    # execute_workflow starts the Workflow and waits for its result.
    result = await client.execute_workflow(
        HelloWorldWorkflow.run,
        "Hello, Temporal + LangGraph!",
        id=f"langgraph-{time.time_ns()}",
        task_queue=TASK_QUEUE,
    )

    print(f"Result: {result}")


if __name__ == "__main__":
    asyncio.run(main())

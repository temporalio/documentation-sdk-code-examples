import asyncio
import time

from temporalio.client import Client
from temporalio.envconfig import ClientConfig

from shared import PAGE_SIZE, TASK_QUEUE
from workflows import IteratorBatchWorkflow


async def main() -> None:
    # Same environment-configuration lookup the Worker uses: TEMPORAL_PROFILE
    # selects the profile, the TOML file supplies address/namespace/credentials,
    # and TEMPORAL_* env vars can override individual fields.
    connect_config = ClientConfig.load_client_connect_config()
    connect_config.setdefault("target_host", "localhost:7233")

    client = await Client.connect(**connect_config)

    handle = await client.start_workflow(
        IteratorBatchWorkflow.run,
        args=[PAGE_SIZE, 0],
        id=f"batch-iterator-{time.time_ns() // 1_000_000}",
        task_queue=TASK_QUEUE,
    )

    address = connect_config.get("target_host")
    namespace = connect_config.get("namespace") or "default"
    print(
        f"Started batch Workflow {handle.id} against {address} "
        f"(namespace: {namespace})..."
    )

    # result() follows the continue-as-new chain and returns the final run's result.
    processed = await handle.result()
    print(f"Batch complete: processed {processed} records")


if __name__ == "__main__":
    asyncio.run(main())

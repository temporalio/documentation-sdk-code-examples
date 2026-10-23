"""Starter that demonstrates standalone Nexus operation execution.

Unlike other Nexus samples that call operations from within a workflow, this
sample executes Nexus operations directly from client code using the standalone
Nexus operation APIs.
"""

import asyncio
import uuid
from datetime import timedelta

from temporalio.client import Client
from temporalio.envconfig import ClientConfig

from nexus_standalone_operations.service import (
    EchoInput,
    HelloInput,
    MyNexusService,
)

ENDPOINT_NAME = "my-nexus-endpoint"


async def main() -> None:
    config = ClientConfig.load_client_connect_config()
    _ = config.setdefault("target_host", "localhost:7233")
    client = await Client.connect(**config)

    # @@@SNIPSTART python-nexus-standalone-execute-operation
    nexus_client = client.create_nexus_client(
        service=MyNexusService, endpoint=ENDPOINT_NAME
    )

    # Await the result of the operation immediately.
    echo_result = await nexus_client.execute_operation(
        MyNexusService.echo,
        EchoInput(message="hello"),
        id=f"echo-{uuid.uuid4()}",
        schedule_to_close_timeout=timedelta(seconds=10),
    )
    # @@@SNIPEND
    print(f"Echo result: {echo_result.message}")

    # @@@SNIPSTART python-nexus-standalone-start-operation
    # Start an operation and get a NexusOperationHandle
    handle = await nexus_client.start_operation(
        MyNexusService.hello,
        HelloInput(name="World"),
        id=f"hello-{uuid.uuid4()}",
        schedule_to_close_timeout=timedelta(seconds=10),
    )
    # Await the result
    try:
        hello_result = await handle.result()
        print(hello_result)
    except Exception as err:
        print(err)
        raise
    # @@@SNIPEND

    print("\nListing Nexus operations:")
    # @@@SNIPSTART python-nexus-standalone-list-operations
    query = f'Endpoint = "{ENDPOINT_NAME}"'
    async for op in client.list_nexus_operations(query):
        print(
            f" OperationId: {op.operation_id},",
            f" Operation: {op.operation},",
            f" Status: {op.status.name}",
        )
    # @@@SNIPEND

    # @@@SNIPSTART python-nexus-standalone-count-operations
    query = f'Endpoint = "{ENDPOINT_NAME}"'
    count = await client.count_nexus_operations(query)
    print(f"Total Nexus operations: {count.count}")
    # @@@SNIPEND


if __name__ == "__main__":
    asyncio.run(main())

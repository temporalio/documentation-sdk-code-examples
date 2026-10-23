import asyncio
import sys

from temporalio.client import Client
from temporalio.envconfig import ClientConfig


async def my_application(run_id: str):
    connect_config = ClientConfig.load_client_connect_config()
    connect_config.setdefault("target_host", "localhost:7233")
    client = await Client.connect(**connect_config)

    # @@@SNIPSTART python-standalone-activity-get-handle
    activity_handle = client.get_activity_handle(
        activity_id="my-standalone-activity-id",
        run_id=run_id,
    )
    # @@@SNIPEND
    print(f"Activity result: {await activity_handle.result()}")


if __name__ == "__main__":
    # Pass the Run ID that start_activity.py printed.
    asyncio.run(my_application(sys.argv[1]))

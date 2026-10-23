# @@@SNIPSTART python-client-load-from-env
import asyncio

from temporalio.client import Client
from temporalio.envconfig import ClientConfig

async def main():
    # load_client_connect_config is a helper that loads a profile and prepares
    # the config dictionary for Client.connect. By default, it loads the
    # "default" profile.
    connect_config = ClientConfig.load_client_connect_config()

    print(f"  Address: {connect_config.get('target_host')}")
    print(f"  Namespace: {connect_config.get('namespace')}")
    print(f"  gRPC Metadata: {connect_config.get('rpc_metadata')}")

    print("\nAttempting to connect to client...")
    try:
        await Client.connect(**connect_config)  # type: ignore
        print("✅ Client connected successfully!")
    except Exception as e:
        print(f"❌ Failed to connect: {e}")


if __name__ == "__main__":
    asyncio.run(main())
# @@@SNIPEND

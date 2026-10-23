from temporalio.client import Client


async def connect() -> Client:
    # @@@SNIPSTART python-cloud-run-connect-profile
    from temporalio.envconfig import ClientConfigProfile

    profile = ClientConfigProfile.load()
    connect_config = profile.to_client_connect_config()
    client = await Client.connect(**connect_config)
    # @@@SNIPEND
    return client

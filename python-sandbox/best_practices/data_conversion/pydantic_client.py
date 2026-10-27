from temporalio.client import Client


async def connect() -> Client:
    # @@@SNIPSTART python-data-conversion-pydantic
    from temporalio.contrib.pydantic import pydantic_data_converter

    client = await Client.connect("localhost:7233", data_converter=pydantic_data_converter)
    # @@@SNIPEND
    return client

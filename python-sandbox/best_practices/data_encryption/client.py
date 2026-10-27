# @@@SNIPSTART python-data-encryption-client
import dataclasses

import temporalio.converter
from temporalio.client import Client

from data_encryption.codec import CompressionCodec

async def connect() -> Client:
    client = await Client.connect(
        "localhost:7233",
        data_converter=dataclasses.replace(
            temporalio.converter.default(), payload_codec=CompressionCodec()
        ),
    )
    # @@@SNIPEND
    return client

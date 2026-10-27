import dataclasses
import os

import aioboto3
from temporalio.client import Client
from temporalio.contrib.aws.s3driver import S3StorageDriver
from temporalio.contrib.aws.s3driver.aioboto3 import new_aioboto3_client
from temporalio.converter import DataConverter, ExternalStorage
from temporalio.envconfig import ClientConfig
from temporalio.worker import Worker

from external_storage.workflow import LargePayloadWorkflow

AWS_PROFILE = os.environ.get("AWS_PROFILE")
AWS_REGION = os.environ.get("AWS_REGION", "us-east-2")


async def run_worker_with_s3() -> None:
    # @@@SNIPSTART python-external-storage-s3-driver-create
    session = aioboto3.Session(profile_name=AWS_PROFILE, region_name=AWS_REGION)
    async with session.client("s3") as s3_client:
        driver = S3StorageDriver(
            client=new_aioboto3_client(s3_client),
            bucket="my-temporal-payloads",
        )
        # @@@SNIPEND

        # @@@SNIPSTART python-external-storage-s3-setup
        data_converter = dataclasses.replace(
            DataConverter.default,
            external_storage=ExternalStorage(drivers=[driver]),
        )

        client_config = ClientConfig.load_client_connect_config()

        client = await Client.connect(**client_config, data_converter=data_converter)

        worker = Worker(
            client,
            task_queue="my-task-queue",
            workflows=[LargePayloadWorkflow],
        )
        # @@@SNIPEND
        await worker.run()


async def create_mrap_driver() -> S3StorageDriver:
    # @@@SNIPSTART python-external-storage-s3-mrap
    session = aioboto3.Session(profile_name=AWS_PROFILE, region_name=AWS_REGION)
    async with session.client("s3") as s3_client:
        driver = S3StorageDriver(
            client=new_aioboto3_client(s3_client),
            bucket="arn:aws:s3::123456789012:accesspoint/mfzwi23gnjvgw.mrap",
        )
        # @@@SNIPEND
        return driver

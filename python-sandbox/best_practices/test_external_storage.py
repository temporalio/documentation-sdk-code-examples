import asyncio
import dataclasses
import os
import uuid

import pytest

pytest.importorskip("aioboto3")
moto_server = pytest.importorskip("moto.server")

import aioboto3
from temporalio.client import Client
from temporalio.contrib.aws.s3driver import S3StorageDriver
from temporalio.contrib.aws.s3driver.aioboto3 import new_aioboto3_client
from temporalio.converter import DataConverter, ExternalStorage
from temporalio.testing import WorkflowEnvironment
from temporalio.worker import Worker

from external_storage import local_disk_driver, multiple_drivers, s3_driver
from external_storage.local_disk_driver import LocalDiskStorageDriver
from external_storage.workflow import LargePayloadWorkflow

pytestmark = pytest.mark.asyncio
REAL_CONNECT = Client.connect
LARGE = "x" * (300 * 1024)  # Larger than the 256 KiB default threshold


async def client_with(env, data_converter) -> Client:
    return await REAL_CONNECT(
        env.client.service_client.config.target_host,
        namespace=env.client.namespace,
        data_converter=data_converter,
    )


async def run_large_workflow(client, task_queue=None, start_worker=True):
    task_queue = task_queue or str(uuid.uuid4())
    if start_worker:
        async with Worker(client, task_queue=task_queue, workflows=[LargePayloadWorkflow]):
            return await client.execute_workflow(
                LargePayloadWorkflow.run, LARGE, id=str(uuid.uuid4()), task_queue=task_queue
            )
    return await client.execute_workflow(
        LargePayloadWorkflow.run, LARGE, id=str(uuid.uuid4()), task_queue=task_queue
    )


async def test_custom_driver_with_zero_threshold(tmp_path):
    driver = LocalDiskStorageDriver(store_dir=str(tmp_path))
    async with await WorkflowEnvironment.start_local() as env:
        client = await client_with(env, local_disk_driver.threshold_data_converter(driver))
        task_queue = str(uuid.uuid4())
        async with Worker(client, task_queue=task_queue, workflows=[LargePayloadWorkflow]):
            result = await client.execute_workflow(
                LargePayloadWorkflow.run, "small", id=str(uuid.uuid4()), task_queue=task_queue
            )
    assert result == "SMALL"
    # With a threshold of 0, even small payloads go to the driver.
    assert any(path.suffix == ".bin" for path in tmp_path.rglob("*"))


async def test_custom_driver_data_converter_offloads_large_payloads():
    async with await WorkflowEnvironment.start_local() as env:
        client = await client_with(env, local_disk_driver.custom_driver_data_converter())
        assert await run_large_workflow(client) == LARGE.upper()


@pytest.fixture
def s3_server(monkeypatch):
    server = moto_server.ThreadedMotoServer(port=0)
    server.start()
    host, port = server.get_host_and_port()
    monkeypatch.setenv("AWS_ENDPOINT_URL", f"http://{host}:{port}")
    monkeypatch.setenv("AWS_ACCESS_KEY_ID", "testing")
    monkeypatch.setenv("AWS_SECRET_ACCESS_KEY", "testing")
    monkeypatch.delenv("AWS_PROFILE", raising=False)
    monkeypatch.setattr(s3_driver, "AWS_PROFILE", None)
    yield
    server.stop()


async def test_s3_driver_worker(s3_server, monkeypatch):
    session = aioboto3.Session(region_name=s3_driver.AWS_REGION)
    async with session.client("s3") as s3:
        await s3.create_bucket(
            Bucket="my-temporal-payloads",
            CreateBucketConfiguration={"LocationConstraint": s3_driver.AWS_REGION},
        )

    async with await WorkflowEnvironment.start_local() as env:
        monkeypatch.setenv("TEMPORAL_ADDRESS", env.client.service_client.config.target_host)
        monkeypatch.setenv("TEMPORAL_NAMESPACE", env.client.namespace)
        worker = asyncio.create_task(s3_driver.run_worker_with_s3())
        try:
            async with session.client("s3") as s3_client:
                driver = S3StorageDriver(
                    client=new_aioboto3_client(s3_client), bucket="my-temporal-payloads"
                )
                client = await client_with(
                    env,
                    dataclasses.replace(
                        DataConverter.default, external_storage=ExternalStorage(drivers=[driver])
                    ),
                )
                result = await run_large_workflow(client, "my-task-queue", start_worker=False)
                assert result == LARGE.upper()
                listed = await s3_client.list_objects_v2(Bucket="my-temporal-payloads")
                assert listed["KeyCount"] > 0
        finally:
            worker.cancel()
            with pytest.raises(asyncio.CancelledError):
                await worker


async def test_multiple_drivers_and_mrap(s3_server):
    session = aioboto3.Session(region_name=s3_driver.AWS_REGION)
    async with session.client("s3") as s3_client:
        external_storage = multiple_drivers.configure(s3_client)
    names = [driver.name() for driver in external_storage.drivers]
    assert names == ["aws.s3driver", "legacy-local-disk"]
    assert external_storage.driver_selector(None, None) is external_storage.drivers[0]

    driver = await s3_driver.create_mrap_driver()
    assert isinstance(driver, S3StorageDriver)

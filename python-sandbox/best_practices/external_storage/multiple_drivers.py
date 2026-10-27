from temporalio.contrib.aws.s3driver import S3StorageDriver
from temporalio.contrib.aws.s3driver.aioboto3 import new_aioboto3_client
from temporalio.converter import ExternalStorage

from external_storage.local_disk_driver import LegacyStorageDriver


def configure(s3_client) -> ExternalStorage:
    # @@@SNIPSTART python-external-storage-multiple-drivers
    preferred_driver = S3StorageDriver(client=new_aioboto3_client(s3_client), bucket="my-bucket")
    legacy_driver = LegacyStorageDriver()

    external_storage = ExternalStorage(
        drivers=[preferred_driver, legacy_driver],
        driver_selector=lambda context, payload: preferred_driver,
    )
    # @@@SNIPEND
    return external_storage

import dataclasses
import os
import uuid
from typing import Sequence

from temporalio.api.common.v1 import Payload
from temporalio.converter import (
    DataConverter,
    ExternalStorage,
    StorageDriver,
    StorageDriverClaim,
    StorageDriverRetrieveContext,
    StorageDriverStoreContext,
    StorageDriverWorkflowInfo,
)


# @@@SNIPSTART python-external-storage-custom-driver
class LocalDiskStorageDriver(StorageDriver):
    def __init__(self, store_dir: str = "/tmp/temporal-payload-store") -> None:
        self._store_dir = store_dir

    def name(self) -> str:
        return "local-disk"

    async def store(
        self,
        context: StorageDriverStoreContext,
        payloads: Sequence[Payload],
    ) -> list[StorageDriverClaim]:
        os.makedirs(self._store_dir, exist_ok=True)

        prefix = self._store_dir
        target = context.target
        if isinstance(target, StorageDriverWorkflowInfo) and target.id:
            prefix = os.path.join(self._store_dir, target.namespace, target.id)
            os.makedirs(prefix, exist_ok=True)

        claims = []
        for payload in payloads:
            key = f"{uuid.uuid4()}.bin"
            file_path = os.path.join(prefix, key)
            with open(file_path, "wb") as f:
                f.write(payload.SerializeToString())
            claims.append(StorageDriverClaim(claim_data={"path": file_path}))
        return claims

    async def retrieve(
        self,
        context: StorageDriverRetrieveContext,
        claims: Sequence[StorageDriverClaim],
    ) -> list[Payload]:
        payloads = []
        for claim in claims:
            file_path = claim.claim_data["path"]
            with open(file_path, "rb") as f:
                raw = f.read()
            payload = Payload()
            payload.ParseFromString(raw)
            payloads.append(payload)
        return payloads
# @@@SNIPEND


def custom_driver_data_converter() -> DataConverter:
    # @@@SNIPSTART python-external-storage-custom-driver-data-converter
    data_converter = dataclasses.replace(
        DataConverter.default,
        external_storage=ExternalStorage(
            drivers=[LocalDiskStorageDriver()],
        ),
    )
    # @@@SNIPEND
    return data_converter


def threshold_data_converter(driver: StorageDriver) -> DataConverter:
    # @@@SNIPSTART python-external-storage-threshold
    data_converter = dataclasses.replace(
        DataConverter.default,
        external_storage=ExternalStorage(
            drivers=[driver],
            payload_size_threshold=0,
        ),
    )
    # @@@SNIPEND
    return data_converter


class LegacyStorageDriver(LocalDiskStorageDriver):
    """A second local disk driver that stands in for an older storage backend."""

    def __init__(self, store_dir: str = "/tmp/temporal-legacy-payload-store") -> None:
        super().__init__(store_dir)

    def name(self) -> str:
        return "legacy-local-disk"

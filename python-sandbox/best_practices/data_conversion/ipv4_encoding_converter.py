# @@@SNIPSTART python-data-conversion-encoding-converter
import dataclasses
import ipaddress
from typing import Any, Optional, Type

from temporalio.api.common.v1 import Payload
from temporalio.converter import (
    CompositePayloadConverter,
    DataConverter,
    DefaultPayloadConverter,
    EncodingPayloadConverter,
)

class IPv4AddressEncodingPayloadConverter(EncodingPayloadConverter):
    @property
    def encoding(self) -> str:
        return "text/ipv4-address"

    def to_payload(self, value: Any) -> Optional[Payload]:
        if isinstance(value, ipaddress.IPv4Address):
            return Payload(
                metadata={"encoding": self.encoding.encode()},
                data=str(value).encode(),
            )
        else:
            return None

    def from_payload(self, payload: Payload, type_hint: Optional[Type] = None) -> Any:
        assert not type_hint or type_hint is ipaddress.IPv4Address
        return ipaddress.IPv4Address(payload.data.decode())

class IPv4AddressPayloadConverter(CompositePayloadConverter):
    def __init__(self) -> None:
        # Just add ours as first before the defaults
        super().__init__(
            IPv4AddressEncodingPayloadConverter(),
            *DefaultPayloadConverter.default_encoding_payload_converters,
        )

my_data_converter = dataclasses.replace(
    DataConverter.default,
    payload_converter_class=IPv4AddressPayloadConverter,
)
# @@@SNIPEND

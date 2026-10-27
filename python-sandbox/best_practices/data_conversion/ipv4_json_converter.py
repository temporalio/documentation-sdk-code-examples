# @@@SNIPSTART python-data-conversion-json-converter
import dataclasses
import inspect
import ipaddress
from typing import Any, Optional, Type, Union

from temporalio.converter import (
    AdvancedJSONEncoder,
    CompositePayloadConverter,
    DataConverter,
    DefaultPayloadConverter,
    JSONPlainPayloadConverter,
    JSONTypeConverter,
    JSONTypeConverterUnhandled,
)

class IPv4AddressJSONEncoder(AdvancedJSONEncoder):
    def default(self, o: Any) -> Any:
        if isinstance(o, ipaddress.IPv4Address):
            return str(o)
        return super().default(o)

class IPv4AddressJSONTypeConverter(JSONTypeConverter):
    def to_typed_value(
        self, hint: Type, value: Any
    ) -> Union[Optional[Any], JSONTypeConverterUnhandled]:
        # Collection hints such as list[IPv4Address] aren't classes, so check first
        if inspect.isclass(hint) and issubclass(hint, ipaddress.IPv4Address):
            return ipaddress.IPv4Address(value)
        return JSONTypeConverter.Unhandled

class IPv4AddressPayloadConverter(CompositePayloadConverter):
    def __init__(self) -> None:
        # Replace default JSON plain with our own that has our encoder and type
        # converter
        json_converter = JSONPlainPayloadConverter(
            encoder=IPv4AddressJSONEncoder,
            custom_type_converters=[IPv4AddressJSONTypeConverter()],
        )
        super().__init__(
            *[
                c if not isinstance(c, JSONPlainPayloadConverter) else json_converter
                for c in DefaultPayloadConverter.default_encoding_payload_converters
            ]
        )

my_data_converter = dataclasses.replace(
    DataConverter.default,
    payload_converter_class=IPv4AddressPayloadConverter,
)
# @@@SNIPEND

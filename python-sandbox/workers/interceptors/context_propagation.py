from contextvars import ContextVar
from typing import Any, Mapping, Protocol

import temporalio.api.common.v1
import temporalio.client
import temporalio.converter
import temporalio.worker

HEADER_KEY = "__my_user_id"

user_id: ContextVar[str | None] = ContextVar("user_id", default=None)


class _InputWithHeaders(Protocol):
    headers: Mapping[str, temporalio.api.common.v1.Payload]


# @@@SNIPSTART python-interceptors-context-propagation
class ContextPropagationInterceptor(
    temporalio.client.Interceptor, temporalio.worker.Interceptor
):
    def __init__(
        self,
        payload_converter: temporalio.converter.PayloadConverter = temporalio.converter.default().payload_converter,
    ) -> None:
        self._payload_converter = payload_converter

    def intercept_client(
        self, next: temporalio.client.OutboundInterceptor
    ) -> temporalio.client.OutboundInterceptor:
        return _ContextPropagationClientOutboundInterceptor(
            next, self._payload_converter
        )

def set_header_from_context(
    input: _InputWithHeaders, payload_converter: temporalio.converter.PayloadConverter
) -> None:
    user_id_val = user_id.get()
    if user_id_val:
        input.headers = {
            **input.headers,
            HEADER_KEY: payload_converter.to_payload(user_id_val),
        }

class _ContextPropagationClientOutboundInterceptor(
    temporalio.client.OutboundInterceptor
):
    def __init__(
        self,
        next: temporalio.client.OutboundInterceptor,
        payload_converter: temporalio.converter.PayloadConverter,
    ) -> None:
        super().__init__(next)
        self._payload_converter = payload_converter

    async def start_workflow(
        self, input: temporalio.client.StartWorkflowInput
    ) -> temporalio.client.WorkflowHandle[Any, Any]:
        set_header_from_context(input, self._payload_converter)
        return await super().start_workflow(input)
# @@@SNIPEND

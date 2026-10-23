# @@@SNIPSTART python-observability-tracing-interceptor
from temporalio.client import Client
from temporalio.contrib.opentelemetry import TracingInterceptor


async def main():
    client = await Client.connect("localhost:7233", interceptors=[TracingInterceptor()])
    # @@@SNIPEND
    return client

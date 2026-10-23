"""Run a Temporal Worker on a Cloud Run worker pool with the Cloud Run Id and
OpenTelemetry plugins."""

import asyncio
from datetime import timedelta

from temporalio.client import Client
from temporalio.contrib.gcp.cloud_run.id import CloudRunIdPlugin
from temporalio.contrib.gcp.cloud_run.opentelemetry import OpenTelemetryPlugin
from temporalio.worker import Worker

from cloud_run_worker.greeting_workflow import GreetingWorkflow, compose_greeting
from cloud_run_worker.settings import load_settings

COLLECTOR_HOST = "127.0.0.1"
COLLECTOR_PORT = 4317
COLLECTOR_STARTUP_TIMEOUT = timedelta(seconds=60)
TRACE_FLUSH_TIMEOUT = timedelta(seconds=2)


async def wait_for_collector() -> None:
    """Wait until the local OTLP gRPC socket accepts connections."""
    loop = asyncio.get_running_loop()
    deadline = loop.time() + COLLECTOR_STARTUP_TIMEOUT.total_seconds()
    while True:
        try:
            _, writer = await asyncio.open_connection(COLLECTOR_HOST, COLLECTOR_PORT)
            writer.close()
            await writer.wait_closed()
            return
        except OSError:
            if loop.time() >= deadline:
                raise
            await asyncio.sleep(0.5)


async def main() -> None:
    settings = load_settings()
    await wait_for_collector()

    # @@@SNIPSTART python-cloud-run-sandbox-otel
    # OpenTelemetryPlugin exports Core metrics and traces to the collector
    # sidecar; CloudRunIdPlugin sets the client identity from instance metadata.
    plugin = OpenTelemetryPlugin(add_temporal_spans=True)
    client = await Client.connect(
        settings.address,
        namespace=settings.namespace,
        api_key=settings.api_key,
        tls=settings.tls,
        plugins=[plugin, CloudRunIdPlugin()],
    )
    # @@@SNIPEND
    worker = Worker(
        client,
        task_queue=settings.task_queue,
        workflows=[GreetingWorkflow],
        activities=[compose_greeting],
    )
    try:
        await worker.run()
    finally:
        # Flush traces after the Worker stops.
        await asyncio.to_thread(plugin.shutdown, TRACE_FLUSH_TIMEOUT)


if __name__ == "__main__":
    asyncio.run(main())

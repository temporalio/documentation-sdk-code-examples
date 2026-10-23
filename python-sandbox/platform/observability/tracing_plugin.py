# @@@SNIPSTART python-observability-opentelemetry-plugin
from opentelemetry import trace
from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from temporalio.client import Client
from temporalio.contrib.opentelemetry import OpenTelemetryPlugin, create_tracer_provider


async def main():
    provider = create_tracer_provider()
    provider.add_span_processor(
        BatchSpanProcessor(OTLPSpanExporter(endpoint="http://localhost:4318/v1/traces"))
    )
    trace.set_tracer_provider(provider)

    client = await Client.connect(
        "localhost:7233",
        plugins=[OpenTelemetryPlugin(add_temporal_spans=True)],
    )
    # @@@SNIPEND
    return client

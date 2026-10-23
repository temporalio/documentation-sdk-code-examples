from temporalio.client import Client


async def connect_with_prometheus() -> Client:
    # @@@SNIPSTART python-observability-prometheus
    from temporalio.runtime import Runtime, TelemetryConfig, PrometheusConfig

    # Create a new runtime that has telemetry enabled. Create this first to avoid
    # the default Runtime from being lazily created.
    new_runtime = Runtime(telemetry=TelemetryConfig(metrics=PrometheusConfig(bind_address="0.0.0.0:9000")))
    my_client = await Client.connect("my.temporal.host:7233", runtime=new_runtime)
    # @@@SNIPEND
    return my_client

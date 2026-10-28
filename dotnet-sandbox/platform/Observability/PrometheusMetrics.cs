// @@@SNIPSTART dotnet-observability-prometheus
using Temporalio.Client;
using Temporalio.Runtime;

public static class PrometheusMetrics
{
    public static async Task RunAsync()
    {
        var runtime = new TemporalRuntime(new()
        {
            Telemetry = new() { Metrics = new() { Prometheus = new("0.0.0.0:9000") } },
        });
        var client = await TemporalClient.ConnectAsync(new("localhost:7233") { Runtime = runtime });
    }
}
// @@@SNIPEND

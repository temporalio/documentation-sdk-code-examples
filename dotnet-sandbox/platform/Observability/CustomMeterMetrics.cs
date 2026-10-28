// @@@SNIPSTART dotnet-observability-custom-meter
using System.Diagnostics.Metrics;
using Temporalio.Client;
using Temporalio.Extensions.DiagnosticSource;
using Temporalio.Runtime;

public static class CustomMeterMetrics
{
    public static async Task RunAsync()
    {
        // Create .NET meter
        using var meter = new Meter("My.Meter");
        // Can create MeterListener or OTel meter provider here...

        // Create Temporal runtime with a custom metric meter for that meter
        var runtime = new TemporalRuntime(new()
        {
            Telemetry = new()
            {
                Metrics = new() { CustomMetricMeter = new CustomMetricMeter(meter) },
            },
        });
        var client = await TemporalClient.ConnectAsync(new("localhost:7233") { Runtime = runtime });
    }
}
// @@@SNIPEND

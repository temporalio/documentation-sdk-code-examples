using Temporalio.Client;
using Temporalio.Runtime;

public static class TelemetryGlobalTagsProgram
{
    public static async Task RunAsync()
    {
        var runtime = new TemporalRuntime(new()
        {
            Telemetry = new()
            {
                Metrics = new()
                {
                    Prometheus = new("0.0.0.0:9000"),
                    GlobalTags = new Dictionary<string, string>
                    {
                        ["team"] = "content-platform",
                        ["service"] = "checkout",
                        ["cost_center"] = "cc-1042",
                        ["environment"] = "production",
                    },
                },
            },
        });
        var client = await TemporalClient.ConnectAsync(
            new("localhost:7233") { Runtime = runtime });
    }
}

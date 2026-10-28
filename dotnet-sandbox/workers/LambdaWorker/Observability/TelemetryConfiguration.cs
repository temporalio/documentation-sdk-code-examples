namespace TemporalioDocs.Workers.LambdaWorker.Observability;

using Temporalio.Extensions.Aws.Lambda;
using Temporalio.Extensions.Aws.Lambda.OpenTelemetry;
using Temporalio.Extensions.OpenTelemetry;
using Temporalio.Runtime;

public static class TelemetryConfiguration
{
    public static void ApplyCustomDefaults(TemporalLambdaWorkerOptions config)
    {
        // @@@SNIPSTART dotnet-lambda-worker-otel-custom-options
        config.ApplyOpenTelemetryDefaults(
            new LambdaWorkerOpenTelemetryOptions
            {
                CollectorEndpoint = "http://localhost:4317",
                ServiceName = "my-worker",
                MetricsExportInterval = TimeSpan.FromSeconds(5),
            });
        // @@@SNIPEND
    }

    public static void ApplyManualTelemetry(TemporalLambdaWorkerOptions config)
    {
        // @@@SNIPSTART dotnet-lambda-worker-manual-telemetry
        config.ClientOptions.Interceptors = new[] { new TracingInterceptor() };
        config.ClientOptions.Runtime = new TemporalRuntime(new TemporalRuntimeOptions
        {
            Telemetry = new TelemetryOptions
            {
                Metrics = new MetricsOptions(new OpenTelemetryOptions("http://collector:4317")),
            },
        });
        // @@@SNIPEND
    }
}

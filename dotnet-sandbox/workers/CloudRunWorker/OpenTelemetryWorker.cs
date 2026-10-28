namespace TemporalioDocs.Workers.CloudRunWorker;

using Temporalio.Client;
using Temporalio.Common.EnvConfig;
using Temporalio.Extensions.Gcp.CloudRun.Id;
using Temporalio.Extensions.Gcp.CloudRun.OpenTelemetry;
using Temporalio.Worker;

public static class OpenTelemetryWorker
{
    public static async Task RunAsync(CancellationToken cancellationToken)
    {
        var connectOptions = ClientEnvConfig.LoadClientConnectOptions();

        // @@@SNIPSTART dotnet-cloud-run-worker-otel
        // The Cloud Run Id plugin sets the client identity to "{instanceId}@{revision}" from Cloud Run
        // metadata at connect time; every Worker created from the client inherits it.
        connectOptions.Plugins = new ITemporalClientPlugin[] { new CloudRunIdPlugin() };

        // ApplyGoogleCloudRunOpenTelemetryDefaults adds the tracing interceptor and a runtime exporting Core
        // metrics + traces over OTLP to the collector sidecar; the returned handle owns the tracer provider.
        // Both plugins configure the same connect options.
        using var telemetry = connectOptions.ApplyGoogleCloudRunOpenTelemetryDefaults();

        var client = await TemporalClient.ConnectAsync(connectOptions);
        // @@@SNIPEND

        var options = new TemporalWorkerOptions(
            Environment.GetEnvironmentVariable("TEMPORAL_TASK_QUEUE")!)
        {
            DeploymentOptions = new(new("my-app", "build-1"), useWorkerVersioning: true),
        };
        options.AddWorkflow<GreetingWorkflow>();
        options.AddAllActivities(typeof(GreetingActivities), null);

        using (var worker = new TemporalWorker(client, options))
        {
            try
            {
                await worker.ExecuteAsync(cancellationToken);
            }
            catch (OperationCanceledException)
            {
            }
        }

        // Flush traces within the Cloud Run termination window.
        await telemetry.FlushAsync(TimeSpan.FromSeconds(2));
    }
}

namespace TemporalioDocs.Activities.StandaloneActivities;

// @@@SNIPSTART dotnet-standalone-activities-worker
using Microsoft.Extensions.Logging;
using Temporalio.Client;
using Temporalio.Common.EnvConfig;
using Temporalio.Worker;

public static class StandaloneWorker
{
    public static async Task RunAsync()
    {
        var connectOptions = ClientEnvConfig.LoadClientConnectOptions();
        connectOptions.TargetHost ??= "localhost:7233";
        connectOptions.LoggerFactory = LoggerFactory.Create(builder =>
            builder.
                AddSimpleConsole(options => options.TimestampFormat = "[HH:mm:ss] ").
                SetMinimumLevel(LogLevel.Information));
        var client = await TemporalClient.ConnectAsync(connectOptions);

        const string taskQueue = "standalone-activity-sample";

        using var tokenSource = new CancellationTokenSource();
        Console.CancelKeyPress += (_, eventArgs) =>
        {
            tokenSource.Cancel();
            eventArgs.Cancel = true;
        };

        using var worker = new TemporalWorker(
            client,
            new TemporalWorkerOptions(taskQueue).
                AddActivity(MyActivities.ComposeGreetingAsync));

        await worker.ExecuteAsync(tokenSource.Token);
    }
}
// @@@SNIPEND

namespace TemporalioDocs.Workers.RunWorker;

using Temporalio.Client;
using Temporalio.Worker;

public static class GracefulShutdown
{
    public static async Task RunAsync(ITemporalClient client)
    {
        // @@@SNIPSTART dotnet-run-worker-graceful-shutdown
        using var tokenSource = new CancellationTokenSource();
        Console.CancelKeyPress += (_, eventArgs) =>
        {
            tokenSource.Cancel();
            eventArgs.Cancel = true;
        };

        var options = new TemporalWorkerOptions("my-task-queue")
        {
            GracefulShutdownTimeout = TimeSpan.FromSeconds(30),
        };
        options.AddWorkflow<GreetingWorkflow>();
        options.AddAllActivities(typeof(GreetingActivities), null);

        using var worker = new TemporalWorker(client, options);
        await worker.ExecuteAsync(tokenSource.Token);
        // @@@SNIPEND
    }
}

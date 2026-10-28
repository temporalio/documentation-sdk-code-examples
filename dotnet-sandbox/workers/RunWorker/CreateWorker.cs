namespace TemporalioDocs.Workers.RunWorker;

using Temporalio.Client;
using Temporalio.Worker;

public static class CreateWorker
{
    public static async Task RunAsync(ITemporalClient client)
    {
        // @@@SNIPSTART dotnet-run-worker-create
        var options = new TemporalWorkerOptions("my-task-queue");
        options.AddWorkflow<GreetingWorkflow>();
        options.AddAllActivities(typeof(GreetingActivities), null);

        using var worker = new TemporalWorker(client, options);
        await worker.ExecuteAsync(CancellationToken.None);
        // @@@SNIPEND
    }
}

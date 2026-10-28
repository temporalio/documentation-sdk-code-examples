namespace TemporalioDocs.Workers.RunWorker;

using Temporalio.Client;
using Temporalio.Common;
using Temporalio.Worker;

public static class VersionedWorker
{
    public static async Task RunAsync(ITemporalClient client, CancellationToken cancellationToken)
    {
        // @@@SNIPSTART dotnet-run-worker-versioned
        var options = new TemporalWorkerOptions("my-task-queue")
        {
            DeploymentOptions = new WorkerDeploymentOptions(
                new WorkerDeploymentVersion("my-app", "1.0"),
                useWorkerVersioning: true),
        };
        options.AddWorkflow<VersionedGreetingWorkflow>();
        options.AddAllActivities(typeof(GreetingActivities), null);

        using var worker = new TemporalWorker(client, options);
        // @@@SNIPEND
        await worker.ExecuteAsync(cancellationToken);
    }
}

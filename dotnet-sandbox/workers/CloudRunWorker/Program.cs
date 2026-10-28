// @@@SNIPSTART dotnet-cloud-run-worker-versioned
using Temporalio.Client;
using Temporalio.Common;
using Temporalio.Worker;
using TemporalioDocs.Workers.CloudRunWorker;

var client = await TemporalClient.ConnectAsync(
    new(Environment.GetEnvironmentVariable("TEMPORAL_ADDRESS")!)
    {
        Namespace = Environment.GetEnvironmentVariable("TEMPORAL_NAMESPACE")!,
        ApiKey = Environment.GetEnvironmentVariable("TEMPORAL_API_KEY"),
        Tls = new(),
    });

var options = new TemporalWorkerOptions(
    Environment.GetEnvironmentVariable("TEMPORAL_TASK_QUEUE")!)
{
    DeploymentOptions = new(new("my-app", "build-1"), useWorkerVersioning: true)
    {
        DefaultVersioningBehavior = VersioningBehavior.Pinned,
    },
};
options.AddWorkflow<GreetingWorkflow>();
options.AddAllActivities(typeof(GreetingActivities), null);

using var worker = new TemporalWorker(client, options);
await worker.ExecuteAsync(CancellationToken.None);
// @@@SNIPEND

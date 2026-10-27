namespace TemporalioDocs.Workflows.Versioning;

using Temporalio.Client;
using Temporalio.Worker;

public static class CutoverWorker
{
    public static async Task RunAsync(ITemporalClient client, CancellationToken cancellationToken)
    {
        // @@@SNIPSTART dotnet-versioning-cutover-worker
        using var worker = new TemporalWorker(
            client,
            new TemporalWorkerOptions("greeting-tasks")
                .AddWorkflow<SayHelloWorkflow>()
                .AddWorkflow<SayHelloWorkflowV2>());
        // @@@SNIPEND
        await worker.ExecuteAsync(cancellationToken);
    }
}

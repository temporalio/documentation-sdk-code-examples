namespace TemporalioDocs.Workflows.WorkflowStreams;

using Temporalio.Client;
using Temporalio.Extensions.WorkflowStreams;
using Temporalio.Workflows;

[Workflow]
public class DeduplicationWindowWorkflow
{
    private readonly WorkflowStream stream;

    [WorkflowInit]
    public DeduplicationWindowWorkflow(OrderInput input)
    {
        // @@@SNIPSTART dotnet-workflow-streams-dedup-workflow
        // Workflow initialization
        stream = new(
            input.StreamState,
            new() { PublisherTtl = TimeSpan.FromMinutes(30) });
        // @@@SNIPEND
    }

    [WorkflowRun]
    public Task RunAsync(OrderInput input) => Workflow.DelayAsync(TimeSpan.FromSeconds(30));
}

public static class DeduplicationWindowClient
{
    public static async Task PublishAsync(ITemporalClient temporalClient, string workflowId)
    {
        // @@@SNIPSTART dotnet-workflow-streams-dedup-client
        // External client or Activity
        await using var streamClient = new WorkflowStreamClient(
            temporalClient,
            workflowId,
            new() { MaxRetryDuration = TimeSpan.FromMinutes(20) });
        // @@@SNIPEND
        streamClient.GetTopic<StatusEvent>("status").Publish(new StatusEvent("started"));
    }
}

namespace TemporalioDocs.Workflows.WorkflowStreams;

// @@@SNIPSTART dotnet-workflow-streams-publish-client
using Temporalio.Client;
using Temporalio.Extensions.WorkflowStreams;

public static class OrderStreamPublisher
{
    public static async Task PublishStatusAsync(
        ITemporalClient temporalClient,
        string workflowId,
        CancellationToken cancellationToken)
    {
        await using var streamClient = new WorkflowStreamClient(
            temporalClient,
            workflowId,
            new()
            {
                BatchInterval = TimeSpan.FromMilliseconds(200),
            });

        var status = streamClient.GetTopic<StatusEvent>("status");
        status.Publish(new StatusEvent("started"));
        status.Publish(new StatusEvent("completed", 100), forceFlush: true);
        await streamClient.FlushAsync(cancellationToken);
    }
}
// @@@SNIPEND

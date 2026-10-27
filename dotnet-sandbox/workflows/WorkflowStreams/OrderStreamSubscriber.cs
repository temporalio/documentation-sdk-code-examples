namespace TemporalioDocs.Workflows.WorkflowStreams;

// @@@SNIPSTART dotnet-workflow-streams-subscribe
using Temporalio.Client;
using Temporalio.Extensions.WorkflowStreams;

public static class OrderStreamSubscriber
{
    public static async Task WatchOrderAsync(
        ITemporalClient temporalClient,
        string workflowId,
        CancellationToken cancellationToken)
    {
        await using var streamClient = new WorkflowStreamClient(temporalClient, workflowId);
        var status = streamClient.GetTopic<StatusEvent>("status");

        await foreach (var item in status.SubscribeAsync().WithCancellation(cancellationToken))
        {
            Console.WriteLine(
                $"[{item.Value.Progress,3}%] {item.Value.State}: {item.Value.Detail}");
            if (item.Value.State == "completed")
            {
                break;
            }
        }
    }
}
// @@@SNIPEND

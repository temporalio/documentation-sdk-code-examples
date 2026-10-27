namespace TemporalioDocs.Workflows.WorkflowStreams;

using Temporalio.Client;
using Temporalio.Extensions.WorkflowStreams;
using Temporalio.Converters;

public static class MultiTopicSubscriber
{
    public static async Task WatchAllTopicsAsync(ITemporalClient temporalClient, string workflowId)
    {
        await using var streamClient = new WorkflowStreamClient(temporalClient, workflowId);

        // @@@SNIPSTART dotnet-workflow-streams-subscribe-heterogeneous
        var options = new WorkflowStreamSubscribeOptions
        {
            Topics = new[] { "status", "progress" },
        };
        var converter = temporalClient.Options.DataConverter.WithSerializationContext(
            new ISerializationContext.Workflow(temporalClient.Options.Namespace, workflowId)).PayloadConverter;

        await foreach (var item in streamClient.SubscribeAsync(options))
        {
            if (item.Topic == "status")
            {
                var evt = converter.ToValue<StatusEvent>(item.Payload);
                Console.WriteLine($"[status] {evt.State}: {evt.Detail}");
            }
            else if (item.Topic == "progress")
            {
                var evt = converter.ToValue<ProgressEvent>(item.Payload);
                Console.WriteLine($"[progress] {evt.Message}");
            }
        }
        // @@@SNIPEND
    }
}

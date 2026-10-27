namespace TemporalioDocs.Workflows.WorkflowStreams.Llm;

// @@@SNIPSTART dotnet-workflow-streams-llm-subscriber
using System.Text;
using Temporalio.Client;
using Temporalio.Converters;
using Temporalio.Extensions.WorkflowStreams;

public static partial class LlmSubscriber
{
    public static async Task<string> StreamResponseAsync(
        ITemporalClient temporalClient,
        string workflowId,
        CancellationToken cancellationToken)
    {
        await using var streamClient = new WorkflowStreamClient(temporalClient, workflowId);
        var converter = temporalClient.Options.DataConverter.WithSerializationContext(
            new ISerializationContext.Workflow(temporalClient.Options.Namespace, workflowId)).PayloadConverter;
        var output = new StringBuilder();
        var options = new WorkflowStreamSubscribeOptions
        {
            Topics = new[] { "delta", "retry", "complete" },
        };

        await foreach (var item in streamClient.SubscribeAsync(options).
            WithCancellation(cancellationToken))
        {
            if (item.Topic == "retry")
            {
                output.Clear();
                Render(output.ToString());
            }
            else if (item.Topic == "delta")
            {
                output.Append(converter.ToValue<TextDelta>(item.Payload).Text);
                Render(output.ToString());
            }
            else if (item.Topic == "complete")
            {
                _ = converter.ToValue<TextComplete>(item.Payload);
                break;
            }
        }

        return output.ToString();
    }
}
// @@@SNIPEND

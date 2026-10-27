namespace TemporalioDocs.Workflows.WorkflowStreams.Llm;

// @@@SNIPSTART dotnet-workflow-streams-llm-activity
using System.ClientModel;
using System.ClientModel.Primitives;
using System.Text;
using OpenAI;
using OpenAI.Chat;
using Temporalio.Activities;
using Temporalio.Extensions.WorkflowStreams;

public record TextDelta(string Text);
public record RetryEvent(int Attempt);
public record TextComplete(string FullText);

public static class LlmActivities
{
    [Activity]
    public static async Task<string> StreamCompletionAsync(LlmInput input)
    {
        var apiKey = Environment.GetEnvironmentVariable("OPENAI_API_KEY");
        if (string.IsNullOrWhiteSpace(apiKey))
        {
            throw new InvalidOperationException("OPENAI_API_KEY must be set");
        }

        var context = ActivityExecutionContext.Current;
        await using var streamClient = WorkflowStreamClient.FromActivity(
            new() { BatchInterval = TimeSpan.FromMilliseconds(200) });

        var deltas = streamClient.GetTopic<TextDelta>("delta");
        var retries = streamClient.GetTopic<RetryEvent>("retry");
        var complete = streamClient.GetTopic<TextComplete>("complete");

        if (context.Info.Attempt > 1)
        {
            retries.Publish(new RetryEvent(context.Info.Attempt), forceFlush: true);
        }

        // Disable provider retries and let the Activity Retry Policy own retries.
        var clientOptions = new OpenAIClientOptions
        {
            RetryPolicy = new ClientRetryPolicy(0),
        };
        var chatClient = new ChatClient(input.Model, new ApiKeyCredential(apiKey), clientOptions);
        var fullText = new StringBuilder();
        var updates = chatClient.CompleteChatStreamingAsync(
            [new UserChatMessage(input.Prompt)],
            cancellationToken: context.CancellationToken);
        await foreach (var update in updates)
        {
            foreach (var contentPart in update.ContentUpdate)
            {
                if (string.IsNullOrEmpty(contentPart.Text))
                {
                    continue;
                }
                deltas.Publish(new TextDelta(contentPart.Text));
                fullText.Append(contentPart.Text);
            }
        }

        var result = fullText.ToString();
        complete.Publish(new TextComplete(result), forceFlush: true);
        await streamClient.FlushAsync();
        return result;
    }
}
// @@@SNIPEND

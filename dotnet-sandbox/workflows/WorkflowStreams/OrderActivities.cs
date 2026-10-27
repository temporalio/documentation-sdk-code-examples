namespace TemporalioDocs.Workflows.WorkflowStreams;

// @@@SNIPSTART dotnet-workflow-streams-publish-activity
using Temporalio.Activities;
using Temporalio.Extensions.WorkflowStreams;

public record ProgressEvent(string Message);

public static class OrderActivities
{
    [Activity]
    public static async Task ProcessOrderAsync(IEnumerable<string> steps)
    {
        await using var streamClient = WorkflowStreamClient.FromActivity();
        var progress = streamClient.GetTopic<ProgressEvent>("progress");

        foreach (var step in steps)
        {
            progress.Publish(new ProgressEvent(step));
            ActivityExecutionContext.Current.Heartbeat(step);
        }
    }
}
// @@@SNIPEND

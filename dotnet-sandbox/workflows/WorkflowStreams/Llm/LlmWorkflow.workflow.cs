namespace TemporalioDocs.Workflows.WorkflowStreams.Llm;

// @@@SNIPSTART dotnet-workflow-streams-llm-workflow
using Temporalio.Extensions.WorkflowStreams;
using Temporalio.Workflows;

public record LlmInput(
    string Prompt,
    string Model = "gpt-4o-mini",
    WorkflowStreamState? StreamState = null);

[Workflow]
public class LlmWorkflow
{
    [WorkflowInit]
    public LlmWorkflow(LlmInput input) => _ = new WorkflowStream(input.StreamState);

    [WorkflowRun]
    public async Task<string> RunAsync(LlmInput input)
    {
        var result = await Workflow.ExecuteActivityAsync(
            () => LlmActivities.StreamCompletionAsync(input),
            new() { StartToCloseTimeout = TimeSpan.FromMinutes(2) });

        // Give the subscriber's in-flight poll time to receive the terminal event.
        await Workflow.DelayAsync(TimeSpan.FromSeconds(30));
        return result;
    }
}
// @@@SNIPEND

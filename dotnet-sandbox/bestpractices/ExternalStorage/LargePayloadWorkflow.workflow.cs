using Temporalio.Workflows;

namespace TemporalioDocs.ExternalStorageExample;

[Workflow]
public class LargePayloadWorkflow
{
    [WorkflowRun]
    public async Task<string> RunAsync(string input) =>
        await Workflow.ExecuteActivityAsync(
            () => LargePayloadActivities.Echo(input),
            new() { StartToCloseTimeout = TimeSpan.FromSeconds(30) });
}

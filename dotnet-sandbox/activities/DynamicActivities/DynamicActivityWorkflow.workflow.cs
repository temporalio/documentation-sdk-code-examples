namespace TemporalioDocs.Activities.DynamicActivities;

using Temporalio.Workflows;

[Workflow]
public class DynamicActivityWorkflow
{
    [WorkflowRun]
    public async Task<string> RunAsync(string name) =>
        // No Activity is registered with this name, so the Dynamic Activity handles it.
        await Workflow.ExecuteActivityAsync<string>(
            "UnregisteredActivity",
            new object?[] { new MyActivityParams("Hello", name) },
            new() { StartToCloseTimeout = TimeSpan.FromMinutes(5) });
}

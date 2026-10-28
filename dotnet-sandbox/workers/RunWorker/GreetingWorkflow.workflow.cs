namespace TemporalioDocs.Workers.RunWorker;

using Temporalio.Workflows;

[Workflow]
public class GreetingWorkflow
{
    [WorkflowRun]
    public async Task<string> RunAsync(string name) =>
        await Workflow.ExecuteActivityAsync(
            () => GreetingActivities.ComposeGreeting(name),
            new() { StartToCloseTimeout = TimeSpan.FromMinutes(1) });
}

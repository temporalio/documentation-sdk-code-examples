namespace TemporalioDocs.Workers.RunWorker;

using Temporalio.Common;
using Temporalio.Workflows;

[Workflow(VersioningBehavior = VersioningBehavior.Pinned)]
public class VersionedGreetingWorkflow
{
    [WorkflowRun]
    public async Task<string> RunAsync(string name) =>
        await Workflow.ExecuteActivityAsync(
            () => GreetingActivities.ComposeGreeting(name),
            new() { StartToCloseTimeout = TimeSpan.FromMinutes(1) });
}

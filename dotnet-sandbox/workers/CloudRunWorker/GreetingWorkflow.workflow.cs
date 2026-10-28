// @@@SNIPSTART dotnet-cloud-run-worker-workflow
namespace TemporalioDocs.Workers.CloudRunWorker;

using Temporalio.Common;
using Temporalio.Workflows;

[Workflow(VersioningBehavior = VersioningBehavior.Pinned)]
public class GreetingWorkflow
{
    [WorkflowRun]
    public async Task<string> RunAsync(string name) =>
        await Workflow.ExecuteActivityAsync(
            () => GreetingActivities.ComposeGreeting(name),
            new() { StartToCloseTimeout = TimeSpan.FromMinutes(1) });
}
// @@@SNIPEND

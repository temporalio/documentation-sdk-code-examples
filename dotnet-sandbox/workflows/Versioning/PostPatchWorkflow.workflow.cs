namespace TemporalioDocs.Workflows.Versioning.PostPatch;

using Temporalio.Workflows;

// @@@SNIPSTART dotnet-versioning-post-patch
[Workflow]
public class MyWorkflow
{
    private string? result;

    [WorkflowRun]
    public async Task RunAsync()
    {
        this.result = await Workflow.ExecuteActivityAsync(
            (MyActivities a) => a.PostPatchActivity(),
            new() { StartToCloseTimeout = TimeSpan.FromMinutes(5) });
    }
}
// @@@SNIPEND

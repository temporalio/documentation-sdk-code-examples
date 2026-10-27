namespace TemporalioDocs.Workflows.Versioning.DeprecatedPatch;

using Temporalio.Workflows;

// @@@SNIPSTART dotnet-versioning-deprecated-patch
[Workflow]
public class MyWorkflow
{
    private string? result;

    [WorkflowRun]
    public async Task RunAsync()
    {
        Workflow.DeprecatePatch("my-patch");
        this.result = await Workflow.ExecuteActivityAsync(
            (MyActivities a) => a.PostPatchActivity(),
            new() { StartToCloseTimeout = TimeSpan.FromMinutes(5) });
    }
}
// @@@SNIPEND

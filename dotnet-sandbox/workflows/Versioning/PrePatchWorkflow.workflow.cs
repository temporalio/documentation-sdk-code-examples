namespace TemporalioDocs.Workflows.Versioning.PrePatch;

using Temporalio.Workflows;

// @@@SNIPSTART dotnet-versioning-pre-patch
[Workflow]
public class MyWorkflow
{
    private string? result;

    [WorkflowRun]
    public async Task RunAsync()
    {
        this.result = await Workflow.ExecuteActivityAsync(
            (MyActivities a) => a.PrePatchActivity(),
            new() { StartToCloseTimeout = TimeSpan.FromMinutes(5) });
    }
}
// @@@SNIPEND

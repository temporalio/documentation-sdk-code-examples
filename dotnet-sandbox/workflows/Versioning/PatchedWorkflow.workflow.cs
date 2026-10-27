namespace TemporalioDocs.Workflows.Versioning.Patched;

using Temporalio.Workflows;

// @@@SNIPSTART dotnet-versioning-patched
[Workflow]
public class MyWorkflow
{
    private string? result;

    [WorkflowRun]
    public async Task RunAsync()
    {
        if (Workflow.Patched("my-patch"))
        {
            this.result = await Workflow.ExecuteActivityAsync(
                (MyActivities a) => a.PostPatchActivity(),
                new() { StartToCloseTimeout = TimeSpan.FromMinutes(5) });
        }
        else
        {
            this.result = await Workflow.ExecuteActivityAsync(
                (MyActivities a) => a.PrePatchActivity(),
                new() { StartToCloseTimeout = TimeSpan.FromMinutes(5) });
        }
    }
}
// @@@SNIPEND

namespace TemporalioDocs.Workflows.WorkflowBasics;

// @@@SNIPSTART dotnet-workflow-basics-custom-type
using Temporalio.Workflows;

[Workflow("MyDifferentWorkflowName")]
public class MyWorkflowWithCustomName
{
    [WorkflowRun]
    public async Task<string> RunAsync(string name)
    {
        var param = new MyActivityParams("Hello", name);
        return await Workflow.ExecuteActivityAsync(
            (MyActivities a) => a.MyActivity(param),
            new() { StartToCloseTimeout = TimeSpan.FromMinutes(5) });
    }
}
// @@@SNIPEND

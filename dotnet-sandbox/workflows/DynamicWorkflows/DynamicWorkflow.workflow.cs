namespace TemporalioDocs.Workflows.DynamicWorkflows;

using Temporalio.Converters;
using Temporalio.Workflows;
using TemporalioDocs.Workflows.WorkflowBasics;

// @@@SNIPSTART dotnet-dynamic-workflow
[Workflow(Dynamic = true)]
public class DynamicWorkflow
{
    [WorkflowRun]
    public async Task<string> RunAsync(IRawValue[] args)
    {
        var name = Workflow.PayloadConverter.ToValue<string>(args.Single());
        var param = new MyActivityParams("Hello", name);
        return await Workflow.ExecuteActivityAsync(
            (MyActivities a) => a.MyActivity(param),
            new() { StartToCloseTimeout = TimeSpan.FromMinutes(5) });
    }
}
// @@@SNIPEND

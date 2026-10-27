namespace TemporalioDocs.Workflows.WorkflowBasics;

using Temporalio.Workflows;

[Workflow]
public class QueryGetterWorkflow
{
    private string someThing = "initial";

    [WorkflowRun]
    public async Task RunAsync(string value)
    {
        someThing = value;
        await Workflow.WaitConditionAsync(() => false);
    }

    // @@@SNIPSTART dotnet-workflow-basics-query-getter
    [WorkflowQuery]
    public string GetSomeThing() => someThing;
    // @@@SNIPEND
}

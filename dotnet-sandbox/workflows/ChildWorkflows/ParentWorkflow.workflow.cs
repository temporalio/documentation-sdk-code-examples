namespace TemporalioDocs.Workflows.ChildWorkflows;

using Temporalio.Workflows;

[Workflow]
public class ParentWorkflow
{
    [WorkflowRun]
    public async Task RunAsync()
    {
        // @@@SNIPSTART dotnet-child-workflows-execute
        await Workflow.ExecuteChildWorkflowAsync((MyChildWorkflow wf) => wf.RunAsync());
        // @@@SNIPEND

        // @@@SNIPSTART dotnet-child-workflows-parent-close-policy
        await Workflow.ExecuteChildWorkflowAsync(
          (MyChildWorkflow wf) => wf.RunAsync(),
          new() { ParentClosePolicy = ParentClosePolicy.Abandon });
        // @@@SNIPEND
    }
}

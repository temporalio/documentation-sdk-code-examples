namespace TemporalioDocs.Activities.AsyncActivityCompletion;

using Temporalio.Workflows;

[Workflow]
public class ApprovalWorkflow
{
    [WorkflowRun]
    public async Task<string> RunAsync() =>
        await Workflow.ExecuteActivityAsync(
            (MyActivities a) => a.RequestApproval(),
            new() { StartToCloseTimeout = TimeSpan.FromMinutes(5) });
}

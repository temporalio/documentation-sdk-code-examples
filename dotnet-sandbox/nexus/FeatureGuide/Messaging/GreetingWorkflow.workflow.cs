namespace TemporalioDocs.Nexus.FeatureGuide.Messaging;

using Temporalio.Workflows;

[Workflow]
public class GreetingWorkflow
{
    private string? approvedBy;

    [WorkflowRun]
    public async Task<string> RunAsync()
    {
        await Workflow.WaitConditionAsync(() => approvedBy != null);
        return $"Approved by {approvedBy}";
    }

    [WorkflowSignal]
    public async Task ApproveAsync(INexusGreetingService.ApproveInput input) => approvedBy = input.Name;
}

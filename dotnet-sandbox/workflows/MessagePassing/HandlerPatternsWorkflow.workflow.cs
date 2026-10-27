namespace TemporalioDocs.Workflows.MessagePassing;

using Temporalio.Workflows;

[Workflow]
public class HandlerPatternsWorkflow
{
    public record UpdateInput(string Name);

    private bool started;

    // @@@SNIPSTART dotnet-message-passing-wait-for-handlers
    [WorkflowRun]
    public async Task<string> RunAsync()
    {
        started = true;
        await Workflow.WaitConditionAsync(() => Workflow.AllHandlersFinished);
        return "workflow-result";
    }
    // @@@SNIPEND

    // @@@SNIPSTART dotnet-message-passing-wait-in-handler
    [WorkflowUpdate]
    public async Task<string> MyUpdateAsync(UpdateInput updateInput)
    {
        await Workflow.WaitConditionAsync(() => ReadyForUpdateToExecute(updateInput));
        return $"Processed {updateInput.Name}";
    }
    // @@@SNIPEND

    // @@@SNIPSTART dotnet-message-passing-unfinished-policy
    [WorkflowUpdate(UnfinishedPolicy = HandlerUnfinishedPolicy.Abandon)]
    public async Task MyAbandonableUpdateAsync()
    {
        await Workflow.DelayAsync(TimeSpan.FromHours(1));
    }
    // @@@SNIPEND

    private bool ReadyForUpdateToExecute(UpdateInput updateInput) => started;
}

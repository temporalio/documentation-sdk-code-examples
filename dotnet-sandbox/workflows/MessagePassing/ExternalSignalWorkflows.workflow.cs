namespace TemporalioDocs.Workflows.MessagePassing;

using Temporalio.Workflows;

[Workflow]
public class WorkflowA
{
    private string? signalArgument;

    [WorkflowRun]
    public async Task<string> RunAsync()
    {
        await Workflow.WaitConditionAsync(() => signalArgument != null);
        return signalArgument!;
    }

    [WorkflowSignal]
    public async Task YourSignalAsync(string argument) => signalArgument = argument;
}

// @@@SNIPSTART dotnet-message-passing-external-signal
[Workflow]
public class WorkflowB
{
    [WorkflowRun]
    public async Task RunAsync()
    {
        var handle = Workflow.GetExternalWorkflowHandle<WorkflowA>("workflow-a");
        await handle.SignalAsync(wf => wf.YourSignalAsync("signal argument"));
    }
}
// @@@SNIPEND

// @@@SNIPSTART dotnet-testing-signal-workflow
namespace TemporalioDocs.BestPractices.TestingSuite;

using Temporalio.Workflows;

[Workflow]
public class SignalWorkflow
{
    private bool signalReceived = false;

    [WorkflowRun]
    public async Task<string> RunAsync()
    {
        // Wait for signal or timeout in 45 seconds
        if (await Workflow.WaitConditionAsync(() => signalReceived, TimeSpan.FromSeconds(45)))
        {
            return "got signal";
        }
        return "got timeout";
    }

    [WorkflowSignal]
    public async Task SomeSignalAsync() => signalReceived = true;
}
// @@@SNIPEND

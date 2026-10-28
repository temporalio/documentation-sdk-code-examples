// @@@SNIPSTART dotnet-testing-wait-a-day-workflow
namespace TemporalioDocs.BestPractices.TestingSuite;

using Temporalio.Workflows;

[Workflow]
public class WaitADayWorkflow
{
    [WorkflowRun]
    public async Task<string> RunAsync()
    {
        await Workflow.DelayAsync(TimeSpan.FromDays(1));
        return "all done";
    }
}
// @@@SNIPEND

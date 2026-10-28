// @@@SNIPSTART dotnet-testing-check-past-histories
namespace TemporalioDocs.BestPractices.TestingSuite;

using System.Runtime.ExceptionServices;
using Temporalio.Client;
using Temporalio.Worker;

public static class CheckPastHistories
{
    public static async Task CheckPastHistoriesAsync(ITemporalClient client)
    {
        var replayer = new WorkflowReplayer(
            new WorkflowReplayerOptions().AddWorkflow<MyWorkflow>());
        var listIter = client.ListWorkflowHistoriesAsync("WorkflowType = 'MyWorkflow'");
        await foreach (var result in replayer.ReplayWorkflowsAsync(listIter))
        {
            if (result.ReplayFailure != null)
            {
                ExceptionDispatchInfo.Throw(result.ReplayFailure);
            }
        }
    }
}
// @@@SNIPEND

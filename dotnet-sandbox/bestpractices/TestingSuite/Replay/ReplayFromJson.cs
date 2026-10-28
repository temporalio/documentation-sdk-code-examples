// @@@SNIPSTART dotnet-testing-replay-from-json
namespace TemporalioDocs.BestPractices.TestingSuite;

using Temporalio.Common;
using Temporalio.Worker;

public static class ReplayFromJson
{
    public static async Task ReplayFromJsonAsync(string historyJson)
    {
        var replayer = new WorkflowReplayer(
            new WorkflowReplayerOptions().AddWorkflow<MyWorkflow>());
        await replayer.ReplayWorkflowAsync(WorkflowHistory.FromJson("my-workflow-id", historyJson));
    }
}
// @@@SNIPEND

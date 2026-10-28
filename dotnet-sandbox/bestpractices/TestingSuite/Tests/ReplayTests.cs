namespace TemporalioDocs.BestPractices.TestingSuite.Tests;

using Temporalio.Client;
using Temporalio.Testing;
using Temporalio.Worker;

// Exercises the replay helpers against history from a real run.
public class ReplayTests
{
    [Fact]
    public async Task ReplayHelpers_CurrentCode_Succeed()
    {
        await using var env = await WorkflowEnvironment.StartLocalAsync();
        using var worker = new TemporalWorker(
            env.Client,
            new TemporalWorkerOptions($"task-queue-{Guid.NewGuid()}").
                AddWorkflow<MyWorkflow>());
        await worker.ExecuteAsync(async () =>
        {
            var handle = await env.Client.StartWorkflowAsync(
                (MyWorkflow wf) => wf.RunAsync("Temporal"),
                new(id: $"wf-{Guid.NewGuid()}", taskQueue: worker.Options.TaskQueue!));
            await handle.GetResultAsync();

            var history = await handle.FetchHistoryAsync();
            await ReplayFromJson.ReplayFromJsonAsync(history.ToJson());
            await CheckPastHistories.CheckPastHistoriesAsync(env.Client);
        });
    }
}

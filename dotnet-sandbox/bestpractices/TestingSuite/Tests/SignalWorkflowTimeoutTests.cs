// @@@SNIPSTART dotnet-testing-signal-timeout-test
namespace TemporalioDocs.BestPractices.TestingSuite.Tests;

using Temporalio.Client;
using Temporalio.Testing;
using Temporalio.Worker;

public class SignalWorkflowTimeoutTests
{
    [Fact]
    public async Task SignalWorkflow_SignalTimeout_HasExpectedResult()
    {
        await using var env = await WorkflowEnvironment.StartTimeSkippingAsync();
        using var worker = new TemporalWorker(
            env.Client,
            new TemporalWorkerOptions($"task-queue-{Guid.NewGuid()}").
                AddWorkflow<SignalWorkflow>());
        await worker.ExecuteAsync(async () =>
        {
            var handle = await env.Client.StartWorkflowAsync(
                (SignalWorkflow wf) => wf.RunAsync(),
                new(id: $"wf-{Guid.NewGuid()}", taskQueue: worker.Options.TaskQueue!));
            await env.DelayAsync(TimeSpan.FromSeconds(50));
            Assert.Equal("got timeout", await handle.GetResultAsync());
        });
    }
}
// @@@SNIPEND

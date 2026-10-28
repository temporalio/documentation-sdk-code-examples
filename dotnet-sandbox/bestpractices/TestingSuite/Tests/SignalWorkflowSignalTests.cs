// @@@SNIPSTART dotnet-testing-signal-test
namespace TemporalioDocs.BestPractices.TestingSuite.Tests;

using Temporalio.Client;
using Temporalio.Testing;
using Temporalio.Worker;

public class SignalWorkflowSignalTests
{
    [Fact]
    public async Task SignalWorkflow_SendSignal_HasExpectedResult()
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
            await handle.SignalAsync(wf => wf.SomeSignalAsync());
            Assert.Equal("got signal", await handle.GetResultAsync());
        });
    }
}
// @@@SNIPEND

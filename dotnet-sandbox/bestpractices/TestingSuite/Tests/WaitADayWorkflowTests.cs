// @@@SNIPSTART dotnet-testing-wait-a-day-test
namespace TemporalioDocs.BestPractices.TestingSuite.Tests;

using Temporalio.Client;
using Temporalio.Testing;
using Temporalio.Worker;

public class WaitADayWorkflowTests
{
    [Fact]
    public async Task WaitADayWorkflow_SimpleRun_Succeeds()
    {
        // Start time-skipping test server
        await using var env = await WorkflowEnvironment.StartTimeSkippingAsync();

        // Create a worker
        using var worker = new TemporalWorker(
          env.Client,
          new TemporalWorkerOptions($"task-queue-{Guid.NewGuid()}").
              AddWorkflow<WaitADayWorkflow>());

        // Run the worker only for the life of the code within
        await worker.ExecuteAsync(async () =>
        {
            // Execute the workflow and confirm the result
            var result = await env.Client.ExecuteWorkflowAsync(
                (WaitADayWorkflow wf) => wf.RunAsync(),
                new(id: $"wf-{Guid.NewGuid()}", taskQueue: worker.Options.TaskQueue!));
            Assert.Equal("all done", result);
        });
    }
}
// @@@SNIPEND

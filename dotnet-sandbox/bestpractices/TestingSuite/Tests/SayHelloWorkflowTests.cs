// @@@SNIPSTART dotnet-testing-say-hello-test
namespace TemporalioDocs.BestPractices.TestingSuite.Tests;

using Temporalio.Client;
using Temporalio.Testing;
using Temporalio.Worker;

public class SayHelloWorkflowTests
{
    [Fact]
    public async Task SayHelloWorkflow_SimpleRun_Succeeds()
    {
        // Start local dev server
        await using var env = await WorkflowEnvironment.StartLocalAsync();

        // Create a worker
        using var worker = new TemporalWorker(
          env.Client,
          new TemporalWorkerOptions($"task-queue-{Guid.NewGuid()}").
              AddWorkflow<SayHelloWorkflow>());

        // Run the worker only for the life of the code within
        await worker.ExecuteAsync(async () =>
        {
            // Execute the workflow and confirm the result
            var result = await env.Client.ExecuteWorkflowAsync(
                (SayHelloWorkflow wf) => wf.RunAsync("Temporal"),
                new(id: $"wf-{Guid.NewGuid()}", taskQueue: worker.Options.TaskQueue!));
            Assert.Equal("Hello, Temporal!", result);
        });
    }
}
// @@@SNIPEND

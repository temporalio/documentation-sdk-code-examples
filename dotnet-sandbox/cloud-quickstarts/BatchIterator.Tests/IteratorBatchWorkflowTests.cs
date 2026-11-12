using Temporalio.Api.Enums.V1;
using Temporalio.Testing;
using Temporalio.Worker;
using TemporalioDocs.CloudQuickstarts.BatchIterator;

namespace TemporalioDocs.CloudQuickstarts.BatchIterator.Tests;

public class IteratorBatchWorkflowTests
{
    [Fact]
    public async Task RunAsync_ProcessesEveryRecordAcrossContinueAsNew()
    {
        // Time skipping makes the children's 1-3 second delays instant.
        await using var env = await WorkflowEnvironment.StartTimeSkippingAsync();
        var taskQueue = Guid.NewGuid().ToString();
        var workflowId = $"batch-iterator-{Guid.NewGuid()}";
        var total = Shared.PageSize * RecordLoader.PageCount;

        using var worker = new TemporalWorker(
            env.Client,
            new TemporalWorkerOptions(taskQueue)
                .AddWorkflow<IteratorBatchWorkflow>()
                .AddWorkflow<RecordProcessorWorkflow>()
                .AddActivity(RecordLoader.GetRecords));

        await worker.ExecuteAsync(async () =>
        {
            var handle = await env.Client.StartWorkflowAsync(
                (IteratorBatchWorkflow wf) => wf.RunAsync(Shared.PageSize, 0),
                new(id: workflowId, taskQueue: taskQueue));

            Assert.Equal(total, await handle.GetResultAsync());

            // The batch continued as new, so the latest run differs from the first.
            var latest = await env.Client.GetWorkflowHandle(workflowId).DescribeAsync();
            Assert.NotEqual(handle.FirstExecutionRunId, latest.RunId);

            // Every record got its own completed child Workflow.
            for (var id = 0; id < total; id++)
            {
                var child = await env.Client.GetWorkflowHandle($"{workflowId}/{id}").DescribeAsync();
                Assert.Equal(WorkflowExecutionStatus.Completed, child.Status);
            }
        });
    }
}

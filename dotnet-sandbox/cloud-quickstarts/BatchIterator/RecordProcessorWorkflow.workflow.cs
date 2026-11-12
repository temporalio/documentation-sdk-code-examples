using Microsoft.Extensions.Logging;
using Temporalio.Workflows;

namespace TemporalioDocs.CloudQuickstarts.BatchIterator;

/// <summary>Child Workflow that processes a single record.</summary>
[Workflow]
public class RecordProcessorWorkflow
{
    [WorkflowRun]
    public async Task RunAsync(SingleRecord record)
    {
        // Simulate 1-3 seconds of work. Kept short so the whole batch finishes in
        // seconds. Workflow.Random is deterministic on replay, unlike System.Random.
        await Workflow.DelayAsync(TimeSpan.FromSeconds(Workflow.Random.Next(1, 4)));
        Workflow.Logger.LogInformation("Processed {Record}", record);
    }
}

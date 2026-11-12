using Temporalio.Workflows;

namespace TemporalioDocs.CloudQuickstarts.BatchIterator;

/// <summary>
/// Implements the iterator pattern.
/// </summary>
/// <remarks>
/// A single Workflow run processes one page of records in parallel, starting one
/// <see cref="RecordProcessorWorkflow"/> child per record. After every child completes,
/// the Workflow continues as new with the next offset, so each run's Event History stays
/// small no matter how large the dataset is.
/// </remarks>
[Workflow]
public class IteratorBatchWorkflow
{
    /// <summary>Processes the batch of records.</summary>
    /// <param name="pageSize">The number of records to process in a single Workflow run.</param>
    /// <param name="offset">The offset of the first record to process. 0 to start the batch.</param>
    /// <returns>Total number of processed records.</returns>
    [WorkflowRun]
    public async Task<int> RunAsync(int pageSize, int offset)
    {
        var records = await Workflow.ExecuteActivityAsync(
            () => RecordLoader.GetRecords(pageSize, offset),
            new() { StartToCloseTimeout = TimeSpan.FromSeconds(5) });

        // No more records in the dataset: the batch is done.
        if (records.Count == 0)
        {
            return offset;
        }

        // Start one child per record with a human-friendly ID, e.g.
        // "batch-iterator-123/7", then wait for every child in this page. Error
        // handling is skipped for brevity.
        await Workflow.WhenAllAsync(records.Select(record =>
            Workflow.ExecuteChildWorkflowAsync(
                (RecordProcessorWorkflow wf) => wf.RunAsync(record),
                new() { Id = $"{Workflow.Info.WorkflowId}/{record.Id}" })));

        throw Workflow.CreateContinueAsNewException(
            (IteratorBatchWorkflow wf) => wf.RunAsync(pageSize, offset + records.Count));
    }
}

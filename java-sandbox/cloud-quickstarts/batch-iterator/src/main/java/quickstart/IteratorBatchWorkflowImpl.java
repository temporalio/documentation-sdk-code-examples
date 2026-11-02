package quickstart;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements the iterator pattern.
 *
 * <p>A single Workflow run processes one page of records in parallel, starting one
 * RecordProcessorWorkflow child per record. After every child completes, the Workflow continues as
 * new with the next offset, so each run's Event History stays small no matter how large the
 * dataset is.
 */
public final class IteratorBatchWorkflowImpl implements IteratorBatchWorkflow {

    private final RecordLoader recordLoader =
            Workflow.newActivityStub(
                    RecordLoader.class,
                    ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(5)).build());

    private final IteratorBatchWorkflow nextRun =
            Workflow.newContinueAsNewStub(IteratorBatchWorkflow.class);

    @Override
    public int processBatch(int pageSize, int offset) {
        List<SingleRecord> records = recordLoader.getRecords(pageSize, offset);

        // No more records in the dataset: the batch is done.
        if (records.isEmpty()) {
            return offset;
        }

        List<Promise<Void>> results = new ArrayList<>(records.size());
        for (SingleRecord record : records) {
            // Human-friendly child ID, e.g. "batch-iterator-123/7".
            String childId = Workflow.getInfo().getWorkflowId() + "/" + record.getId();
            RecordProcessorWorkflow processor =
                    Workflow.newChildWorkflowStub(
                            RecordProcessorWorkflow.class,
                            ChildWorkflowOptions.newBuilder().setWorkflowId(childId).build());
            results.add(Async.procedure(processor::processRecord, record));
        }
        // Wait for every child in this page. Error handling is skipped for brevity.
        Promise.allOf(results).get();

        return nextRun.processBatch(pageSize, offset + records.size());
    }
}

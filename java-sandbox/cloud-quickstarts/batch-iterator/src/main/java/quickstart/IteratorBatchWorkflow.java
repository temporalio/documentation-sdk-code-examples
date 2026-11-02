package quickstart;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface IteratorBatchWorkflow {
    /**
     * Processes the batch of records.
     *
     * @param pageSize the number of records to process in a single Workflow run.
     * @param offset the offset of the first record to process. 0 to start the batch.
     * @return total number of processed records.
     */
    @WorkflowMethod
    int processBatch(int pageSize, int offset);
}

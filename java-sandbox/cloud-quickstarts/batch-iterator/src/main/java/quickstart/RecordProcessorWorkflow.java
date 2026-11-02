package quickstart;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface RecordProcessorWorkflow {
    /** Processes a single record. */
    @WorkflowMethod
    void processRecord(SingleRecord record);
}

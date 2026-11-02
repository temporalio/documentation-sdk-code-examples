package quickstart;

import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.Random;
import org.slf4j.Logger;

public class RecordProcessorWorkflowImpl implements RecordProcessorWorkflow {
    private static final Logger log = Workflow.getLogger(RecordProcessorWorkflowImpl.class);

    // Workflow.newRandom() is deterministic on replay, unlike java.util.Random.
    private final Random random = Workflow.newRandom();

    @Override
    public void processRecord(SingleRecord record) {
        // Simulate 1-3 seconds of work. Kept short so the whole batch finishes in seconds.
        Workflow.sleep(Duration.ofSeconds(1 + random.nextInt(3)));
        log.info("Processed {}", record);
    }
}

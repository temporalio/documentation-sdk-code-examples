package io.temporal.docs.activities.execution;

import io.temporal.docs.activities.execution.FileProcessing.Arguments;
import io.temporal.docs.activities.execution.FileProcessing.FileProcessingActivities;
import io.temporal.docs.activities.execution.FileProcessing.FileProcessingWorkflow;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class MultipleStubsWorkflowImpl implements FileProcessingWorkflow {

    private final FileProcessingActivities store1;
    private final FileProcessingActivities store2;

    // @@@SNIPSTART java-activity-execution-multiple-stubs
    public MultipleStubsWorkflowImpl() {
        ActivityOptions options1 = ActivityOptions.newBuilder()
                 .setTaskQueue("taskQueue1")
                 .setStartToCloseTimeout(Duration.ofMinutes(10))
                 .build();
        this.store1 = Workflow.newActivityStub(FileProcessingActivities.class, options1);

        ActivityOptions options2 = ActivityOptions.newBuilder()
                 .setTaskQueue("taskQueue2")
                 .setStartToCloseTimeout(Duration.ofMinutes(5))
                 .build();
        this.store2 = Workflow.newActivityStub(FileProcessingActivities.class, options2);
    }
    // @@@SNIPEND

    @Override
    public void processFile(Arguments args) {
        String localName = store1.download(args.getSourceBucketName(), args.getSourceFilename());
        store2.upload(args.getTargetBucketName(), args.getTargetFilename(), localName);
    }
}

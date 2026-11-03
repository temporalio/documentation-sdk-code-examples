package io.temporal.docs.activities.execution;

import io.temporal.docs.activities.execution.FileProcessing.Arguments;
import io.temporal.docs.activities.execution.FileProcessing.FileProcessingActivities;
import io.temporal.docs.activities.execution.FileProcessing.FileProcessingWorkflow;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

// @@@SNIPSTART java-activity-execution-sync
public class SyncFileProcessingWorkflowImpl implements FileProcessingWorkflow {

    private final FileProcessingActivities activities;

    public SyncFileProcessingWorkflowImpl() {
        this.activities =
            Workflow.newActivityStub(
                FileProcessingActivities.class,
                ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofHours(1))
                    .build());
    }

    @Override
    public void processFile(Arguments args) {
        String localName = null;
        String processedName = null;
        try {
            localName = activities.download(args.getSourceBucketName(), args.getSourceFilename());
            processedName = activities.processFile(localName);
            activities.upload(args.getTargetBucketName(), args.getTargetFilename(), processedName);
        } finally {
            if (localName != null) {
                activities.deleteLocalFile(localName);
            }
            if (processedName != null) {
                activities.deleteLocalFile(processedName);
            }
        }
    }
}
// @@@SNIPEND

package io.temporal.docs.workflows.versioning;

import io.temporal.activity.ActivityOptions;
import io.temporal.docs.workflows.versioning.FileProcessing.Arguments;
import io.temporal.docs.workflows.versioning.FileProcessing.FileProcessingActivities;
import io.temporal.docs.workflows.versioning.FileProcessing.FileProcessingWorkflow;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class FileProcessingWorkflowOriginal implements FileProcessingWorkflow {

    private final FileProcessingActivities activities = Workflow.newActivityStub(
        FileProcessingActivities.class,
        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

    @Override
    // @@@SNIPSTART java-versioning-original
    public void processFile(Arguments args) {
        String localName = null;
        String processedName = null;
        try {
            localName = activities.download(args.getSourceBucketName(), args.getSourceFilename());
            processedName = activities.processFile(localName);
            activities.upload(args.getTargetBucketName(), args.getTargetFilename(), processedName);
        } finally {
            if (localName != null) { // File was downloaded.
                activities.deleteLocalFile(localName);
            }
            if (processedName != null) { // File was processed.
                activities.deleteLocalFile(processedName);
            }
        }
    }
    // @@@SNIPEND
}

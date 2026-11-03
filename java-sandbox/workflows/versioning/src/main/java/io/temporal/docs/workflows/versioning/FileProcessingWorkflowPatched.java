package io.temporal.docs.workflows.versioning;

import io.temporal.activity.ActivityOptions;
import io.temporal.docs.workflows.versioning.FileProcessing.Arguments;
import io.temporal.docs.workflows.versioning.FileProcessing.FileProcessingActivities;
import io.temporal.docs.workflows.versioning.FileProcessing.FileProcessingWorkflow;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class FileProcessingWorkflowPatched implements FileProcessingWorkflow {

    private final FileProcessingActivities activities = Workflow.newActivityStub(
        FileProcessingActivities.class,
        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

    @Override
    // @@@SNIPSTART java-versioning-get-version
    public void processFile(Arguments args) {
        String localName = null;
        String processedName = null;
        try {
            localName = activities.download(args.getSourceBucketName(), args.getSourceFilename());
            processedName = activities.processFile(localName);
            int version = Workflow.getVersion("checksumAdded", Workflow.DEFAULT_VERSION, 1);
            if (version == Workflow.DEFAULT_VERSION) {
                activities.upload(args.getTargetBucketName(), args.getTargetFilename(), processedName);
            } else {
                long checksum = activities.calculateChecksum(processedName);
                activities.uploadWithChecksum(
                    args.getTargetBucketName(), args.getTargetFilename(), processedName, checksum);
            }
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

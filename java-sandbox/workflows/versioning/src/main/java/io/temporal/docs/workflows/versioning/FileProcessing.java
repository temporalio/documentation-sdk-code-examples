package io.temporal.docs.workflows.versioning;

import io.temporal.activity.ActivityInterface;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

public class FileProcessing {

    public static class Arguments {
        private String sourceBucketName;
        private String sourceFilename;
        private String targetBucketName;
        private String targetFilename;

        public Arguments() {}

        public Arguments(String sourceBucketName, String sourceFilename, String targetBucketName, String targetFilename) {
            this.sourceBucketName = sourceBucketName;
            this.sourceFilename = sourceFilename;
            this.targetBucketName = targetBucketName;
            this.targetFilename = targetFilename;
        }

        public String getSourceBucketName() {
            return sourceBucketName;
        }

        public String getSourceFilename() {
            return sourceFilename;
        }

        public String getTargetBucketName() {
            return targetBucketName;
        }

        public String getTargetFilename() {
            return targetFilename;
        }
    }

    @ActivityInterface
    public interface FileProcessingActivities {
        String download(String bucketName, String filename);

        String processFile(String localName);

        void upload(String bucketName, String filename, String localName);

        long calculateChecksum(String localName);

        void uploadWithChecksum(String bucketName, String filename, String localName, long checksum);

        void deleteLocalFile(String localName);
    }

    @WorkflowInterface
    public interface FileProcessingWorkflow {
        @WorkflowMethod
        void processFile(Arguments args);
    }
}

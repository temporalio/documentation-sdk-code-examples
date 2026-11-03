package io.temporal.docs.activities.execution;

import io.temporal.activity.ActivityInterface;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.List;

public class FileProcessing {

    public static class Arguments {
        private String sourceBucketName;
        private String sourceFilename;
        private List<String> sourceFilenames;
        private String targetBucketName;
        private String targetFilename;

        public Arguments() {}

        public Arguments(String sourceBucketName, List<String> sourceFilenames, String targetBucketName, String targetFilename) {
            this.sourceBucketName = sourceBucketName;
            this.sourceFilename = sourceFilenames.get(0);
            this.sourceFilenames = sourceFilenames;
            this.targetBucketName = targetBucketName;
            this.targetFilename = targetFilename;
        }

        public String getSourceBucketName() {
            return sourceBucketName;
        }

        public String getSourceFilename() {
            return sourceFilename;
        }

        public List<String> getSourceFilenames() {
            return sourceFilenames;
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

        List<String> processFiles(List<String> localNames);

        void upload(String bucketName, String filename, String localName);

        void deleteLocalFile(String localName);
    }

    @WorkflowInterface
    public interface FileProcessingWorkflow {
        @WorkflowMethod
        void processFile(Arguments args);
    }
}

package io.temporal.docs.activities.execution;

import io.temporal.docs.activities.execution.FileProcessing.Arguments;
import io.temporal.docs.activities.execution.FileProcessing.FileProcessingActivities;
import io.temporal.docs.activities.execution.FileProcessing.FileProcessingWorkflow;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class AsyncFileProcessingWorkflowImpl implements FileProcessingWorkflow {

    private final FileProcessingActivities activities = Workflow.newActivityStub(
        FileProcessingActivities.class,
        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(1)).build());

    // @@@SNIPSTART java-activity-execution-async-parallel
    public void processFile(Arguments args) {
        List<Promise<String>> localNamePromises = new ArrayList<>();
        List<String> processedNames = null;
        try {
            // Download all files in parallel.
            for (String sourceFilename : args.getSourceFilenames()) {
                Promise<String> localName =
                    Async.function(activities::download, args.getSourceBucketName(), sourceFilename);
                localNamePromises.add(localName);
            }
            List<String> localNames = new ArrayList<>();
            for (Promise<String> localName : localNamePromises) {
                localNames.add(localName.get());
            }
            processedNames = activities.processFiles(localNames);

            // Upload all results in parallel.
            List<Promise<Void>> uploadedList = new ArrayList<>();
            for (String processedName : processedNames) {
                Promise<Void> uploaded =
                    Async.procedure(
                        activities::upload,
                        args.getTargetBucketName(),
                        args.getTargetFilename(),
                        processedName);
                uploadedList.add(uploaded);
            }
            // Wait for all uploads to complete.
            Promise.allOf(uploadedList).get();
        } finally {
            for (Promise<String> localNamePromise : localNamePromises) {
                // Skip files that haven't completed downloading.
                if (localNamePromise.isCompleted()) {
                    activities.deleteLocalFile(localNamePromise.get());
                }
            }
            if (processedNames != null) {
                for (String processedName : processedNames) {
                    activities.deleteLocalFile(processedName);
                }
            }
        }
    }
    // @@@SNIPEND

    /** Starts one download asynchronously, as in the single-call example. */
    public static String downloadOne(FileProcessingActivities activities, String sourceBucket, String sourceFile) {
        // @@@SNIPSTART java-activity-execution-async-function
        Promise<String> localNamePromise = Async.function(activities::download, sourceBucket, sourceFile);
        // @@@SNIPEND
        return localNamePromise.get();
    }
}

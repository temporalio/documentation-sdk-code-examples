package io.temporal.docs.activities.execution.asynccompletion;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface FileProcessingActivities {
    String download(String bucketName, String remoteName, String localName);
}

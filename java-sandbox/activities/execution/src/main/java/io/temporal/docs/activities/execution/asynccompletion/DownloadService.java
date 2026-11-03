package io.temporal.docs.activities.execution.asynccompletion;

import io.temporal.client.ActivityCompletionClient;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Downloads files in the background and completes the waiting Activity when each one finishes. */
public class DownloadService {
  private final ActivityCompletionClient completionClient;

  public DownloadService(ActivityCompletionClient completionClient) {
    this.completionClient = completionClient;
  }

  public void startDownload(byte[] taskToken, String bucketName, String remoteName, String localPath) {
    CompletableFuture.runAsync(
        () -> {
          if (remoteName.startsWith("missing")) {
            failActivity(taskToken, new IllegalArgumentException("No such file: " + remoteName));
          } else {
            completeActivity(taskToken, localPath);
          }
        },
        CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS));
  }

  // @@@SNIPSTART java-activity-execution-complete-from-another-process
  public <R> void completeActivity(byte[] taskToken, R result) {
    completionClient.complete(taskToken, result);
  }

  public void failActivity(byte[] taskToken, Exception failure) {
    completionClient.completeExceptionally(taskToken, failure);
  }
  // @@@SNIPEND
}

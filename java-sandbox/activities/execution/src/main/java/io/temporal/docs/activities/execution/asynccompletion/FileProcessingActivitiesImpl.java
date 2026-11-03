package io.temporal.docs.activities.execution.asynccompletion;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;

// @@@SNIPSTART java-activity-execution-do-not-complete-on-return
public class FileProcessingActivitiesImpl implements FileProcessingActivities {

  private final DownloadService downloadService;
  private final String localDirectory = "/tmp/";

  public FileProcessingActivitiesImpl(DownloadService downloadService) {
    this.downloadService = downloadService;
  }

  public String download(String bucketName, String remoteName, String localName) {

    ActivityExecutionContext ctx = Activity.getExecutionContext();

    // Used to correlate reply
    byte[] taskToken = ctx.getInfo().getTaskToken();

    asyncDownloadFileFromS3(taskToken, bucketName, remoteName, localDirectory + localName);
    ctx.doNotCompleteOnReturn();

    // Return value is ignored when doNotCompleteOnReturn was called.
    return "ignored";
  }

  private void asyncDownloadFileFromS3(byte[] taskToken, String bucketName, String remoteName, String localPath) {
    // Starts the download and returns right away; the service completes the Activity later.
    downloadService.startDownload(taskToken, bucketName, remoteName, localPath);
  }
}
// @@@SNIPEND

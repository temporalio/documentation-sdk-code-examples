package io.temporal.docs.activities.execution.context;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;
import io.temporal.activity.ActivityInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// @@@SNIPSTART java-activity-execution-context
public class FileProcessingActivitiesImpl implements FileProcessingActivities {

  private static final Logger log = LoggerFactory.getLogger(FileProcessingActivitiesImpl.class);
  private final String localDirectory = "/tmp/";

  @Override
  public String download(String bucketName, String remoteName, String localName) {

    ActivityExecutionContext ctx = Activity.getExecutionContext();
    ActivityInfo info = ctx.getInfo();

    log.info("namespace=" +  info.getActivityNamespace());
    log.info("workflowId=" + info.getWorkflowId());
    log.info("runId=" + info.getRunId());
    log.info("activityId=" + info.getActivityId());
    log.info("activityTimeout=" + info.getStartToCloseTimeout());

    return downloadFileFromS3(bucketName, remoteName, localDirectory + localName);
  }

  private String downloadFileFromS3(String bucketName, String remoteName, String localPath) {
    // Download s3://bucketName/remoteName to localPath.
    return localPath;
  }
}
// @@@SNIPEND

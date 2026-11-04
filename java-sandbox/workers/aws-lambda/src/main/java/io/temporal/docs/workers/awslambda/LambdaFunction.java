// @@@SNIPSTART java-aws-lambda-function
package io.temporal.docs.workers.awslambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.temporal.aws.lambda.LambdaWorker;
import io.temporal.common.WorkerDeploymentVersion;

/** AWS Lambda entry point for the Temporal Worker. */
public class LambdaFunction implements RequestHandler<Object, Void> {

  private static final RequestHandler<Object, Void> WORKER =
      LambdaWorker.define(
          new WorkerDeploymentVersion("my-app", "build-1"),
          builder -> {
            builder.setTaskQueue("my-task-queue");
            builder.registerWorkflowImplementationTypes(SampleWorkflowImpl.class);
            builder.registerActivitiesImplementations(new GreetingActivitiesImpl());
          });

  @Override
  public Void handleRequest(Object input, Context context) {
    return WORKER.handleRequest(input, context);
  }
}
// @@@SNIPEND

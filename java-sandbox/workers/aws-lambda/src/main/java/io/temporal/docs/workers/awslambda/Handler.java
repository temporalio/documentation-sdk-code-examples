package io.temporal.docs.workers.awslambda;

// @@@SNIPSTART java-aws-lambda-otel-handler
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.temporal.aws.lambda.LambdaWorker;
import io.temporal.aws.lambda.OtelLambdaWorkerConfigurationHelper;
import io.temporal.common.WorkerDeploymentVersion;

public final class Handler implements RequestHandler<Object, Void> {
  private static final RequestHandler<Object, Void> WORKER =
      LambdaWorker.define(
          new WorkerDeploymentVersion("my-app", "build-1"),
          builder -> {
            OtelLambdaWorkerConfigurationHelper.configure(builder);
            builder.setTaskQueue("serverless-task-queue-1");
            builder.registerWorkflowImplementationTypes(SampleWorkflowImpl.class);
            builder.registerActivitiesImplementations(new GreetingActivitiesImpl());
          });

  @Override
  public Void handleRequest(Object input, Context context) {
    return WORKER.handleRequest(input, context);
  }
}
// @@@SNIPEND

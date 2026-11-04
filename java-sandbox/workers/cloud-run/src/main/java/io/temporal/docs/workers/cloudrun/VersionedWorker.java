// @@@SNIPSTART java-cloud-run-versioned-worker
package io.temporal.docs.workers.cloudrun;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.common.VersioningBehavior;
import io.temporal.common.WorkerDeploymentVersion;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerDeploymentOptions;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;

public final class VersionedWorker {
  public static void main(String[] args) {
    String apiKey = System.getenv("TEMPORAL_API_KEY");

    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder()
                .setTarget(System.getenv("TEMPORAL_ADDRESS"))
                .setEnableHttps(true)
                .addApiKey(() -> apiKey)
                .build());

    WorkflowClient client =
        WorkflowClient.newInstance(
            service,
            WorkflowClientOptions.newBuilder()
                .setNamespace(System.getenv("TEMPORAL_NAMESPACE"))
                .build());

    WorkerFactory factory = WorkerFactory.newInstance(client);

    Worker worker =
        factory.newWorker(
            System.getenv("TEMPORAL_TASK_QUEUE"),
            WorkerOptions.newBuilder()
                .setDeploymentOptions(
                    WorkerDeploymentOptions.newBuilder()
                        .setUseVersioning(true)
                        .setVersion(new WorkerDeploymentVersion("my-app", "build-1"))
                        .setDefaultVersioningBehavior(VersioningBehavior.PINNED)
                        .build())
                .build());

    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());

    factory.start();
  }
}
// @@@SNIPEND

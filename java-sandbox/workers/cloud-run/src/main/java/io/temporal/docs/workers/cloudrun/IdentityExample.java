package io.temporal.docs.workers.cloudrun;

// @@@SNIPSTART java-cloud-run-worker-identity
import io.temporal.client.WorkflowClient;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.gcp.cloudrun.id.CloudRunIdPlugin;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import java.io.IOException;

public final class IdentityExample {
  public static WorkflowClient newClient() throws IOException {
    ClientConfigProfile profile = ClientConfigProfile.load();
    WorkflowServiceStubsOptions serviceOptions =
        WorkflowServiceStubsOptions.newBuilder(profile.toWorkflowServiceStubsOptions())
            .setPlugins(new CloudRunIdPlugin())
            .build();
    WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(serviceOptions);
    return WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());
  }
}
// @@@SNIPEND

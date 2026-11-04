package io.temporal.docs.client.temporalclient;

// @@@SNIPSTART java-temporal-client-load-from-env
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.envconfig.LoadClientConfigProfileOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadFromEnv {

  private static final Logger logger = LoggerFactory.getLogger(LoadFromEnv.class);

  public static void main(String[] args) {
    try {

      ClientConfigProfile profile =
          ClientConfigProfile.load(LoadClientConfigProfileOptions.newBuilder().build());

      WorkflowServiceStubsOptions serviceStubsOptions = profile.toWorkflowServiceStubsOptions();
      WorkflowClientOptions clientOptions = profile.toWorkflowClientOptions();

      try {
        // Create the workflow client using the loaded configuration
        WorkflowClient client =
            WorkflowClient.newInstance(
                WorkflowServiceStubs.newServiceStubs(serviceStubsOptions), clientOptions);

        // Test the connection by getting system info
        var systemInfo =
            client
                .getWorkflowServiceStubs()
                .blockingStub()
                .getSystemInfo(
                    io.temporal.api.workflowservice.v1.GetSystemInfoRequest.getDefaultInstance());

        logger.info("✅ Client connected successfully!");
        logger.info("   Server version: {}", systemInfo.getServerVersion());

      } catch (Exception e) {
        logger.error("❌ Failed to connect: {}", e.getMessage());
      }

    } catch (Exception e) {
      logger.error("Failed to load configuration: {}", e.getMessage(), e);
      System.exit(1);
    }
  }
}
// @@@SNIPEND

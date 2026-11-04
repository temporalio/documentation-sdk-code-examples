package io.temporal.docs.client.temporalclient;

// @@@SNIPSTART java-temporal-client-load-profile
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.envconfig.LoadClientConfigProfileOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadProfile {

  private static final Logger logger = LoggerFactory.getLogger(LoadProfile.class);

  public static void main(String[] args) {
    String profileName = "cloud";

    try {
      String configFilePath =
          Paths.get(LoadProfile.class.getResource("/config.toml").toURI()).toString();

      logger.info("--- Loading '{}' profile from {} ---", profileName, configFilePath);

      // Load specific profile from file with environment variable overrides
      ClientConfigProfile profile =
          ClientConfigProfile.load(
              LoadClientConfigProfileOptions.newBuilder()
                  .setConfigFilePath(configFilePath)
                  .setConfigFileProfile(profileName)
                  .build());

      // Demonstrate programmatic override - set the Namespace in code instead of the file
      ClientConfigProfile.Builder profileBuilder = profile.toBuilder();
      profileBuilder.setNamespace("your-namespace.a1b2c"); // Override the Namespace from the file
      profile = profileBuilder.build();

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

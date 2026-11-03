package io.temporal.docs.activities.standaloneactivities;

import static io.temporal.docs.activities.standaloneactivities.StandaloneActivityWorker.TASK_QUEUE;

import io.temporal.client.ActivityClient;
import io.temporal.client.ActivityClientOptions;
import io.temporal.client.StartActivityOptions;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.io.IOException;
import java.time.Duration;

/** Executes the standalone activity by its type name instead of a method reference. */
public class ExecuteActivityByTypeName {

  public static void main(String[] args) throws IOException {
    ClientConfigProfile profile = ClientConfigProfile.load();
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());

    ActivityClient client =
        ActivityClient.newInstance(
            service,
            ActivityClientOptions.newBuilder().setNamespace(profile.getNamespace()).build());

    StartActivityOptions options =
        StartActivityOptions.newBuilder()
            .setId("standalone-activity-by-name-id")
            .setTaskQueue(TASK_QUEUE)
            .setStartToCloseTimeout(Duration.ofSeconds(10))
            .build();

    try {
      // @@@SNIPSTART java-standalone-activities-execute-by-name
      // Using a string type name
      String result = client.execute("ComposeGreeting", String.class, options, "Hello", "World");
      // @@@SNIPEND
      System.out.println("Activity result: " + result);
    } finally {
      service.shutdown();
    }
  }
}

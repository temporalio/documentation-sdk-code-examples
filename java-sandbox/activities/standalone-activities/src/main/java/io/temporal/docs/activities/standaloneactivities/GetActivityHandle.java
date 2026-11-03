package io.temporal.docs.activities.standaloneactivities;

import io.temporal.client.ActivityClient;
import io.temporal.client.ActivityClientOptions;
import io.temporal.client.ActivityHandle;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.util.concurrent.CompletableFuture;

/**
 * Gets a handle to the standalone activity that StartActivity or ExecuteActivity started, then
 * waits for its result. Run one of those first.
 */
public class GetActivityHandle {

  public static void main(String[] args) throws Exception {
    ClientConfigProfile profile = ClientConfigProfile.load();
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());

    ActivityClient client =
        ActivityClient.newInstance(
            service,
            ActivityClientOptions.newBuilder().setNamespace(profile.getNamespace()).build());

    try {
      // @@@SNIPSTART java-standalone-activities-get-handle
      ActivityHandle<String> handle =
          client.getHandle("standalone-activity-id", null, String.class);
      // @@@SNIPEND

      // @@@SNIPSTART java-standalone-activities-get-result
      String result = handle.getResult();
      // @@@SNIPEND
      System.out.println("Activity result: " + result);

      // @@@SNIPSTART java-standalone-activities-get-result-async
      CompletableFuture<String> future = handle.getResultAsync();
      // @@@SNIPEND
      System.out.println("Activity result (async): " + future.get());
    } finally {
      service.shutdown();
    }
  }
}

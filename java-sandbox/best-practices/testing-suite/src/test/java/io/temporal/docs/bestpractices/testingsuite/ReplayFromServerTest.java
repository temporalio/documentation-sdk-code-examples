package io.temporal.docs.bestpractices.testingsuite;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * The in-memory test server doesn't implement ListWorkflowExecutions, so this test needs a dev
 * server ({@code temporal server start-dev}). Set TEMPORAL_ADDRESS to use an address other than
 * localhost:7233. The test is skipped when no server is reachable.
 */
public class ReplayFromServerTest {

  @Test
  public void replayHistoriesFromTheServer() throws Exception {
    String address = System.getenv().getOrDefault("TEMPORAL_ADDRESS", "localhost:7233");
    WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(
        WorkflowServiceStubsOptions.newBuilder().setTarget(address).build());
    try {
      boolean reachable;
      try {
        service.healthCheck();
        reachable = true;
      } catch (RuntimeException e) {
        reachable = false;
      }
      assumeTrue(reachable, "No Temporal Service at " + address);

      WorkflowClient client = WorkflowClient.newInstance(service);
      WorkerFactory factory = WorkerFactory.newInstance(client);
      Worker worker = factory.newWorker("mytaskqueue");
      worker.registerWorkflowImplementationTypes(SayHelloWorkflowImpl.class);
      worker.registerActivitiesImplementations(new ReplayTest.FastGreetActivities());
      factory.start();
      try {
        // The Workflow sleeps for a day, so start it and let it wait on its Timer: replay works on
        // open Workflow Executions as well as closed ones.
        SayHelloWorkflow workflow = client.newWorkflowStub(SayHelloWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("replay-" + UUID.randomUUID())
                .setTaskQueue("mytaskqueue")
                .build());
        WorkflowClient.start(workflow::sayHello, "Temporal");
        // Visibility is eventually consistent.
        Thread.sleep(2000);

        ReplayExamples.replayFromServer(service, client);
      } finally {
        factory.shutdownNow();
      }
    } finally {
      service.shutdownNow();
    }
  }
}

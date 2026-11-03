package io.temporal.docs.workflows.continueasnew;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.client.WorkflowUpdateException;
import io.temporal.client.WorkflowUpdateStage;
import io.temporal.failure.ApplicationFailure;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ClusterManagerWorkflowTest {

  private static final String TASK_QUEUE = "cluster-manager";

  private TestWorkflowEnvironment env;
  private WorkflowClient client;

  @BeforeEach
  public void setUp() {
    env = TestWorkflowEnvironment.newInstance();
    Worker worker = env.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(ClusterManagerWorkflowImpl.class);
    worker.registerActivitiesImplementations(new ClusterManagerActivitiesImpl());
    env.start();
    client = env.getWorkflowClient();
  }

  @AfterEach
  public void tearDown() {
    env.close();
  }

  private ClusterManagerWorkflow newCluster(String workflowId) {
    return client.newWorkflowStub(
        ClusterManagerWorkflow.class,
        WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId(workflowId).build());
  }

  @Test
  public void continuesAsNewAndKeepsState() throws Exception {
    ClusterManagerWorkflow cluster = newCluster("cluster-continue-as-new");
    WorkflowExecution firstRun =
        WorkflowClient.start(
            cluster::run, new ClusterManagerWorkflow.ClusterManagerInput(Optional.empty(), true));

    cluster.startCluster();
    cluster.assignNodesToJobs(
        new ClusterManagerWorkflow.ClusterManagerAssignNodesToJobInput(2, "job"));

    // With the test hook on, the Workflow continues as new once its history passes 120 events.
    env.sleep(Duration.ofMinutes(2));

    WorkflowStub latestStub = client.newUntypedWorkflowStub("cluster-continue-as-new");
    String latestRunId = latestStub.describe().getExecution().getRunId();
    assertNotEquals(firstRun.getRunId(), latestRunId);

    // The new run received the state, so the cluster is still started and the job is still assigned.
    client.newWorkflowStub(ClusterManagerWorkflow.class, "cluster-continue-as-new").stopCluster();
    ClusterManagerWorkflow.ClusterManagerResult result =
        latestStub.getResult(ClusterManagerWorkflow.ClusterManagerResult.class);
    assertEquals(2, result.getNumCurrentlyAssignedNodes() + result.getNumBadNodes());
  }

  @Test
  public void assignsAndDeletesJobs() throws Exception {
    ClusterManagerWorkflow cluster = newCluster("cluster-safe-messages");
    CompletableFuture<ClusterManagerWorkflow.ClusterManagerResult> result =
        WorkflowClient.execute(
            cluster::run, new ClusterManagerWorkflow.ClusterManagerInput(Optional.empty(), false));

    cluster.startCluster();

    List<CompletableFuture<ClusterManagerWorkflow.ClusterManagerAssignNodesToJobResult>>
        assignJobs = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      assignJobs.add(
          WorkflowStub.fromTyped(cluster)
              .startUpdate(
                  "assignNodesToJobs",
                  WorkflowUpdateStage.ACCEPTED,
                  ClusterManagerWorkflow.ClusterManagerAssignNodesToJobResult.class,
                  new ClusterManagerWorkflow.ClusterManagerAssignNodesToJobInput(2, "job-" + i))
              .getResultAsync());
    }
    for (CompletableFuture<ClusterManagerWorkflow.ClusterManagerAssignNodesToJobResult> f :
        assignJobs) {
      assertEquals(2, f.get().getNodesAssigned().size());
    }

    env.sleep(Duration.ofSeconds(1));

    List<CompletableFuture<Void>> deleteJobs = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      deleteJobs.add(
          WorkflowStub.fromTyped(cluster)
              .startUpdate(
                  "deleteJob",
                  WorkflowUpdateStage.ACCEPTED,
                  Void.class,
                  new ClusterManagerWorkflow.ClusterManagerDeleteJobInput("job-" + i))
              .getResultAsync());
    }
    deleteJobs.forEach(CompletableFuture::join);

    cluster.stopCluster();
    assertEquals(0, result.get().getNumCurrentlyAssignedNodes());
  }

  @Test
  public void assignIsIdempotent() {
    ClusterManagerWorkflow cluster = newCluster("cluster-idempotency");
    WorkflowClient.execute(
        cluster::run, new ClusterManagerWorkflow.ClusterManagerInput(Optional.empty(), false));

    cluster.startCluster();

    ClusterManagerWorkflow.ClusterManagerAssignNodesToJobResult result1 =
        cluster.assignNodesToJobs(
            new ClusterManagerWorkflow.ClusterManagerAssignNodesToJobInput(5, "test-job"));
    ClusterManagerWorkflow.ClusterManagerAssignNodesToJobResult result2 =
        cluster.assignNodesToJobs(
            new ClusterManagerWorkflow.ClusterManagerAssignNodesToJobInput(5, "test-job"));

    assertTrue(result1.getNodesAssigned().size() >= result2.getNodesAssigned().size());
  }

  @Test
  public void assignFailsWhenNotEnoughNodes() throws Exception {
    ClusterManagerWorkflow cluster = newCluster("cluster-update-failure");
    CompletableFuture<ClusterManagerWorkflow.ClusterManagerResult> result =
        WorkflowClient.execute(
            cluster::run, new ClusterManagerWorkflow.ClusterManagerInput(Optional.empty(), false));

    cluster.startCluster();

    cluster.assignNodesToJobs(
        new ClusterManagerWorkflow.ClusterManagerAssignNodesToJobInput(24, "big-job"));
    WorkflowUpdateException updateFailure =
        assertThrows(
            WorkflowUpdateException.class,
            () ->
                cluster.assignNodesToJobs(
                    new ClusterManagerWorkflow.ClusterManagerAssignNodesToJobInput(
                        3, "little-job")));
    assertTrue(updateFailure.getCause() instanceof ApplicationFailure);
    assertEquals(
        "Cannot assign nodes to a job: Not enough nodes available",
        ((ApplicationFailure) updateFailure.getCause()).getOriginalMessage());

    cluster.stopCluster();
    assertEquals(24, result.get().getNumCurrentlyAssignedNodes() + result.get().getNumBadNodes());
  }
}

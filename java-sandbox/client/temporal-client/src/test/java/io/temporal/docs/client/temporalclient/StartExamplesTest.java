package io.temporal.docs.client.temporalclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.common.SearchAttributeKey;
import io.temporal.docs.client.temporalclient.Workflows.CronWorkflow;
import io.temporal.docs.client.temporalclient.Workflows.CronWorkflowImpl;
import io.temporal.docs.client.temporalclient.Workflows.DynamicGreetingWorkflowImpl;
import io.temporal.docs.client.temporalclient.Workflows.FileProcessingWorkflow;
import io.temporal.docs.client.temporalclient.Workflows.FileProcessingWorkflowImpl;
import io.temporal.docs.client.temporalclient.Workflows.NotifyUserAccountsImpl;
import io.temporal.docs.client.temporalclient.Workflows.OrderWorkflow;
import io.temporal.docs.client.temporalclient.Workflows.OrderWorkflowImpl;
import io.temporal.docs.client.temporalclient.Workflows.YourWorkflow;
import io.temporal.docs.client.temporalclient.Workflows.YourWorkflowImpl;
import io.temporal.docs.client.temporalclient.Workflows.YourWorkflowInterface;
import io.temporal.docs.client.temporalclient.Workflows.YourWorkflowSignalingImpl;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StartExamplesTest {

  private static final String TASK_QUEUE = StartExamples.TASK_QUEUE;

  private TestWorkflowEnvironment testEnv;
  private WorkflowClient client;

  @BeforeEach
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    client = testEnv.getWorkflowClient();
    Worker worker = testEnv.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(
        NotifyUserAccountsImpl.class,
        FileProcessingWorkflowImpl.class,
        YourWorkflowImpl.class,
        DynamicGreetingWorkflowImpl.class,
        OrderWorkflowImpl.class,
        YourWorkflowSignalingImpl.class,
        CronWorkflowImpl.class);
    testEnv.start();
  }

  @AfterEach
  public void tearDown() {
    testEnv.close();
  }

  @Test
  public void startSync() {
    StartExamples.startSync(client, TASK_QUEUE);
  }

  @Test
  public void startTypedThenCallStubAgainWaitsForResult() {
    FileProcessingWorkflow workflow =
        StartExamples.startTyped(client, TASK_QUEUE, "typed", "a.txt");
    // Calling the Workflow method on the started stub waits for the existing Execution.
    assertEquals("Processed a.txt", workflow.processFile("a.txt"));
  }

  @Test
  public void startUntypedThenGetResultById() {
    StartExamples.startUntyped(client, TASK_QUEUE, "untyped", "b.txt");
    assertEquals("Processed b.txt", StartExamples.resultUntyped(client, "untyped"));
  }

  @Test
  public void resultSync() {
    assertEquals("Processed c.txt", StartExamples.resultSync(client, TASK_QUEUE, "sync", "c.txt"));
  }

  @Test
  public void resultByWorkflowId() {
    StartExamples.startTyped(client, TASK_QUEUE, "by-id", "d.txt");
    assertEquals("Processed d.txt", StartExamples.resultById(client, "by-id"));
  }

  @Test
  public void dynamicWorkflow() {
    WorkflowStub workflow = StartExamples.dynamicStub(client, "dynamic");
    workflow.start("World");
    assertEquals("DynamicWF: Hello World!", workflow.getResult(String.class));
  }

  @Test
  public void externalWorkflowStubSignalsOtherWorkflow() {
    OrderWorkflow order =
        client.newWorkflowStub(
            OrderWorkflow.class,
            WorkflowOptions.newBuilder().setWorkflowId("OtherWFId").setTaskQueue(TASK_QUEUE).build());
    WorkflowClient.start(order::run);

    YourWorkflow caller =
        client.newWorkflowStub(
            YourWorkflow.class, WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());
    assertEquals("Signaled OtherWFId", caller.yourWFMethod("book"));
    assertEquals(List.of("book"), order.run());
  }

  @Test
  public void lastCompletionResultIsNullOnFirstRun() {
    CronWorkflow workflow =
        client.newWorkflowStub(
            CronWorkflow.class, WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());
    assertNull(workflow.cronWorkflow());
  }

  private static WorkflowOptions options(YourWorkflowInterface workflow) {
    return WorkflowStub.fromTyped(workflow).getOptions().get();
  }

  @Test
  public void workflowOptionsExamples() {
    assertEquals(TASK_QUEUE, options(StartExamples.taskQueue(client)).getTaskQueue());
    assertEquals("YourWF", options(StartExamples.workflowId(client)).getWorkflowId());
    assertEquals(
        Duration.ofSeconds(10),
        options(StartExamples.executionTimeout(client)).getWorkflowExecutionTimeout());
    assertEquals(
        Duration.ofSeconds(10), options(StartExamples.runTimeout(client)).getWorkflowRunTimeout());
    assertEquals(
        Duration.ofSeconds(10), options(StartExamples.taskTimeout(client)).getWorkflowTaskTimeout());
    assertEquals(
        3, options(StartExamples.retryOptions(client)).getRetryOptions().getMaximumAttempts());
    assertEquals("memoValue", options(StartExamples.memo(client)).getMemo().get("memoKey"));
    assertEquals(
        "value",
        options(StartExamples.searchAttributes(client))
            .getTypedSearchAttributes()
            .get(SearchAttributeKey.forKeyword("CustomKeywordField")));
  }

  @Test
  public void rejectDuplicateBlocksSecondRun() {
    YourWorkflowInterface first = StartExamples.idReusePolicy(client);
    assertEquals(
        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE,
        options(first).getWorkflowIdReusePolicy());
    assertEquals("done", first.yourMethod());

    YourWorkflowInterface second = StartExamples.idReusePolicy(client);
    assertThrows(WorkflowExecutionAlreadyStarted.class, () -> WorkflowClient.start(second::yourMethod));
  }

  @Test
  public void connectToNamespace() {
    WorkflowClient namespaced =
        ConnectExamples.connectToNamespace(testEnv.getWorkflowServiceStubs(), "default");
    assertEquals("default", namespaced.getOptions().getNamespace());
  }
}

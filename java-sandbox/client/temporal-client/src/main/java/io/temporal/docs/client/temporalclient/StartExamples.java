package io.temporal.docs.client.temporalclient;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.common.RetryOptions;
import io.temporal.common.SearchAttributeKey;
import io.temporal.common.SearchAttributes;
import io.temporal.docs.client.temporalclient.Workflows.FileProcessingWorkflow;
import io.temporal.docs.client.temporalclient.Workflows.NotifyUserAccounts;
import io.temporal.docs.client.temporalclient.Workflows.YourWorkflowInterface;
import java.time.Duration;
import java.util.Map;

/** Ways to start a Workflow Execution, set WorkflowOptions, and get its result. */
public class StartExamples {

  static final String TASK_QUEUE = "temporal-client";

  public static void startSync(WorkflowClient client, String taskQueue) {
    // @@@SNIPSTART java-temporal-client-start-sync
    NotifyUserAccounts workflow =
        client.newWorkflowStub(
            NotifyUserAccounts.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("notifyAccounts")
                .setTaskQueue(taskQueue)
                .build());

    // Start the Workflow and wait for a result.
    // notify(String[] accountIds) is a Workflow method defined in the Workflow Definition.
    workflow.notify(
        new String[] {
          "Account1", "Account2", "Account3", "Account4", "Account5",
          "Account6", "Account7", "Account8", "Account9", "Account10"
        });
    // @@@SNIPEND
  }

  public static FileProcessingWorkflow startTyped(
      WorkflowClient client, String taskQueue, String workflowId, String fileName) {
    // @@@SNIPSTART java-temporal-client-start-typed
    // Create a typed Workflow stub
    FileProcessingWorkflow workflow =
        client.newWorkflowStub(
            FileProcessingWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(taskQueue)
                .setWorkflowId(workflowId)
                .build());
    // Use WorkflowClient.execute to return a future that contains the Workflow result or failure,
    // or use WorkflowClient.start to return the Workflow ID and Run ID of the started Workflow.
    WorkflowExecution execution = WorkflowClient.start(workflow::processFile, fileName);
    // @@@SNIPEND
    return workflow;
  }

  public static WorkflowStub startUntyped(
      WorkflowClient client, String taskQueue, String workflowId, String argument) {
    // @@@SNIPSTART java-temporal-client-start-untyped
    WorkflowStub untyped =
        client.newUntypedWorkflowStub(
            "FileProcessingWorkflow",
            WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(taskQueue)
                .build());

    // Blocks until the Workflow Execution has been started (not until it completes)
    untyped.start(argument);
    // @@@SNIPEND
    return untyped;
  }

  public static WorkflowStub dynamicStub(WorkflowClient client, String workflowId) {
    // @@@SNIPSTART java-temporal-client-dynamic-stub
    // The Worker must register the Dynamic Workflow implementation, for example:
    // worker.registerWorkflowImplementationTypes(DynamicGreetingWorkflowImpl.class);

    // Create the Workflow stub to call the Dynamic Workflow.
    // The "DynamicWF" Workflow Type is not explicitly registered with the Worker.
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId(workflowId).build();
    WorkflowStub workflow = client.newUntypedWorkflowStub("DynamicWF", workflowOptions);
    // @@@SNIPEND
    return workflow;
  }

  public static YourWorkflowInterface taskQueue(WorkflowClient client) {
    // @@@SNIPSTART java-temporal-client-task-queue
    // Create a Workflow stub for YourWorkflowInterface
    YourWorkflowInterface workflow1 =
        client.newWorkflowStub(
            YourWorkflowInterface.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("YourWF")
                // Set the Task Queue
                .setTaskQueue(TASK_QUEUE)
                .build());
    // @@@SNIPEND
    return workflow1;
  }

  public static YourWorkflowInterface workflowId(WorkflowClient client) {
    // @@@SNIPSTART java-temporal-client-workflow-id
    // Create a Workflow stub for YourWorkflowInterface
    YourWorkflowInterface workflow1 =
        client.newWorkflowStub(
            YourWorkflowInterface.class,
            WorkflowOptions.newBuilder()
                // Set the Workflow ID
                .setWorkflowId("YourWF")
                .setTaskQueue(TASK_QUEUE)
                .build());
    // @@@SNIPEND
    return workflow1;
  }

  public static YourWorkflowInterface executionTimeout(WorkflowClient client) {
    // @@@SNIPSTART java-temporal-client-execution-timeout
    // Create a Workflow stub for YourWorkflowInterface
    YourWorkflowInterface workflow1 =
        client.newWorkflowStub(
            YourWorkflowInterface.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("YourWF")
                .setTaskQueue(TASK_QUEUE)
                // Set Workflow Execution Timeout duration
                .setWorkflowExecutionTimeout(Duration.ofSeconds(10))
                .build());
    // @@@SNIPEND
    return workflow1;
  }

  public static YourWorkflowInterface runTimeout(WorkflowClient client) {
    // @@@SNIPSTART java-temporal-client-run-timeout
    // Create a Workflow stub for YourWorkflowInterface
    YourWorkflowInterface workflow1 =
        client.newWorkflowStub(
            YourWorkflowInterface.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("YourWF")
                .setTaskQueue(TASK_QUEUE)
                // Set Workflow Run Timeout duration
                .setWorkflowRunTimeout(Duration.ofSeconds(10))
                .build());
    // @@@SNIPEND
    return workflow1;
  }

  public static YourWorkflowInterface taskTimeout(WorkflowClient client) {
    // @@@SNIPSTART java-temporal-client-task-timeout
    // Create a Workflow stub for YourWorkflowInterface
    YourWorkflowInterface workflow1 =
        client.newWorkflowStub(
            YourWorkflowInterface.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("YourWF")
                .setTaskQueue(TASK_QUEUE)
                // Set Workflow Task Timeout duration
                .setWorkflowTaskTimeout(Duration.ofSeconds(10))
                .build());
    // @@@SNIPEND
    return workflow1;
  }

  public static YourWorkflowInterface idReusePolicy(WorkflowClient client) {
    // @@@SNIPSTART java-temporal-client-id-reuse-policy
    // Create a Workflow stub for YourWorkflowInterface
    YourWorkflowInterface workflow1 =
        client.newWorkflowStub(
            YourWorkflowInterface.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("YourWF")
                .setTaskQueue(TASK_QUEUE)
                // Set Workflow ID Reuse Policy
                .setWorkflowIdReusePolicy(
                    WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build());
    // @@@SNIPEND
    return workflow1;
  }

  public static YourWorkflowInterface retryOptions(WorkflowClient client) {
    // @@@SNIPSTART java-temporal-client-retry-options
    // Create a Workflow stub for YourWorkflowInterface
    YourWorkflowInterface workflow1 =
        client.newWorkflowStub(
            YourWorkflowInterface.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("YourWF")
                .setTaskQueue(TASK_QUEUE)
                // Set Workflow Retry Options
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
    // @@@SNIPEND
    return workflow1;
  }

  public static YourWorkflowInterface memo(WorkflowClient client) {
    // @@@SNIPSTART java-temporal-client-memo
    // Create a Workflow stub for YourWorkflowInterface
    YourWorkflowInterface workflow1 =
        client.newWorkflowStub(
            YourWorkflowInterface.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("YourWF")
                .setTaskQueue(TASK_QUEUE)
                // Set Memo. You can set additional non-indexed info via Memo
                .setMemo(Map.of("memoKey", "memoValue"))
                .build());
    // @@@SNIPEND
    return workflow1;
  }

  public static YourWorkflowInterface searchAttributes(WorkflowClient client) {
    // @@@SNIPSTART java-temporal-client-search-attributes
    // Create a Workflow stub for YourWorkflowInterface
    YourWorkflowInterface workflow1 =
        client.newWorkflowStub(
            YourWorkflowInterface.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("YourWF")
                .setTaskQueue(TASK_QUEUE)
                // Set Search Attributes
                .setTypedSearchAttributes(
                    SearchAttributes.newBuilder()
                        .set(SearchAttributeKey.forKeyword("CustomKeywordField"), "value")
                        .build())
                .build());
    // @@@SNIPEND
    return workflow1;
  }

  public static String resultSync(
      WorkflowClient client, String taskQueue, String workflowId, String fileName) {
    // @@@SNIPSTART java-temporal-client-result-sync
    FileProcessingWorkflow workflow =
        client.newWorkflowStub(
            FileProcessingWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(taskQueue)
                .build());

    // Start sync and wait for results (or failure)
    String result = workflow.processFile(fileName);
    // @@@SNIPEND
    return result;
  }

  public static String resultById(WorkflowClient client, String workflowId) {
    // @@@SNIPSTART java-temporal-client-result-by-id
    FileProcessingWorkflow workflow = client.newWorkflowStub(FileProcessingWorkflow.class, workflowId);

    // Returns the result after waiting for the Workflow to complete.
    String result = workflow.processFile(null);
    // @@@SNIPEND
    return result;
  }

  public static String resultUntyped(WorkflowClient client, String workflowId) {
    // @@@SNIPSTART java-temporal-client-result-untyped
    WorkflowStub workflowStub = client.newUntypedWorkflowStub(workflowId);

    // Returns the result after waiting for the Workflow to complete.
    String result = workflowStub.getResult(String.class);
    // @@@SNIPEND
    return result;
  }
}

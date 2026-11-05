package io.temporal.docs.bestpractices.testingsuite;

import io.temporal.api.workflowservice.v1.GetWorkflowExecutionHistoryRequest;
import io.temporal.api.workflowservice.v1.GetWorkflowExecutionHistoryResponse;
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsRequest;
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.common.WorkflowExecutionHistory;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.testing.WorkflowReplayer;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class ReplayExamples {

  public static void replayFromServer(WorkflowServiceStubs service, WorkflowClient client)
      throws Exception {
    // @@@SNIPSTART java-testing-suite-replay-from-server
    // Note we assume you already have a WorkflowServiceStubs (`service`) and WorkflowClient (`client`)
    // in scope.
    ListWorkflowExecutionsRequest listWorkflowExecutionRequest =
        ListWorkflowExecutionsRequest.newBuilder()
            .setNamespace(client.getOptions().getNamespace())
            .setQuery("TaskQueue = 'mytaskqueue'")
            .build();
    ListWorkflowExecutionsResponse listWorkflowExecutionsResponse =
        service.blockingStub().listWorkflowExecutions(listWorkflowExecutionRequest);
    List<WorkflowExecutionHistory> histories =
        listWorkflowExecutionsResponse.getExecutionsList().stream()
            .map(
                (info) -> {
                  GetWorkflowExecutionHistoryResponse weh =
                      service.blockingStub().getWorkflowExecutionHistory(
                          GetWorkflowExecutionHistoryRequest.newBuilder()
                              .setNamespace(client.getOptions().getNamespace())
                              .setExecution(info.getExecution())
                              .build());
                  return new WorkflowExecutionHistory(
                      weh.getHistory(), info.getExecution().getWorkflowId());
                })
            .collect(Collectors.toList());

    WorkflowReplayer.replayWorkflowExecutions(
        histories, true, SayHelloWorkflowImpl.class, CancellableGreetingWorkflowImpl.class);
    // @@@SNIPEND
  }

  public static void replayFromFile() throws Exception {
    // @@@SNIPSTART java-testing-suite-replay-from-file
    File file = new File("my_history.json");
    WorkflowReplayer.replayWorkflowExecution(file, SayHelloWorkflowImpl.class);
    // @@@SNIPEND
  }
}

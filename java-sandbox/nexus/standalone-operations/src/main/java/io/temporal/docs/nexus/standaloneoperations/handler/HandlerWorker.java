package io.temporal.docs.nexus.standaloneoperations.handler;

import io.temporal.client.WorkflowClient;
import io.temporal.docs.nexus.standaloneoperations.service.ClientOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

// Worker that hosts the Nexus service implementation and the workflow backing its operation. The
// task queue must match the Nexus endpoint's target task queue (see README).
public class HandlerWorker {
  public static final String TASK_QUEUE_NAME = "nexus-handler-queue";

  public static void main(String[] args) {
    WorkflowClient client = ClientOptions.getWorkflowClient();

    WorkerFactory factory = WorkerFactory.newInstance(client);

    Worker worker = factory.newWorker(TASK_QUEUE_NAME);
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
    worker.registerNexusServiceImplementation(new GreetingNexusServiceImpl());

    factory.start();
  }
}

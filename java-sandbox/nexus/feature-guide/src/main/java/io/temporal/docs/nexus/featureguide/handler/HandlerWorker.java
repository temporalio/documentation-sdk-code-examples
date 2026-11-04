// @@@SNIPSTART java-nexus-feature-guide-handler-worker
package io.temporal.docs.nexus.featureguide.handler;

import io.temporal.client.WorkflowClient;
import io.temporal.docs.nexus.featureguide.options.ClientOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class HandlerWorker {
  public static final String DEFAULT_TASK_QUEUE_NAME = "my-handler-task-queue";

  public static void main(String[] args) {
    WorkflowClient client = ClientOptions.getWorkflowClient(args);

    WorkerFactory factory = WorkerFactory.newInstance(client);

    Worker worker = factory.newWorker(DEFAULT_TASK_QUEUE_NAME);
    worker.registerWorkflowImplementationTypes(HelloHandlerWorkflowImpl.class);
    worker.registerNexusServiceImplementation(new SampleNexusServiceImpl());

    factory.start();
  }
}
// @@@SNIPEND
